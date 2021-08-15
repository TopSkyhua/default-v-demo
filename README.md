@[TOC](Jackson 自定义注解实现null值自定义序列化)

# Jackson 自定义注解实现null值自定义序列化

spring项目中都使用的是Jackson为默认的序列化方式，但是不免有时不满足项目的需要，例如以下场景：

1. 返回前端时部分字段不能为null，需要默认值
2. 某类型的字段需要默认值
3. 某字段需要特殊默认值

综上各类场景，总而言之就是要对为 `null` 值的字段进行一些默认值赋值处理，让返回的json中不存在未null的字段，针对此需求无非两种解决方式：一是在业务中手动编码进行各字段赋值（当然部分也需要这么做），二是进行序列化方式配置，让系统在序列化时自动进行赋值

第一种方式的弊端我想大家都能一目了然（重复、烦杂），今天我们主讲第二种实现方式

## 目前简单的方式

1. 直接使用fastjson，配置简单，内置配置即可实现
2. 使用Jackson，使用本次介绍的方式

对比：
使用fastjson的方式配置比较简单，使用也方便，能满足需求，但是本身不够灵活，因为是配置全局生效，要排除的字段需要单独指定序列化方式

使用本次介绍的方式灵活度很高，默认为原本的序列化方式（null 序列化还是 null），不做任何处理，而针对极少场景再使用`自定义注解`改变默认序列化方式，让null值序列化为默认值

## 原理
开发自定义的null值序列化方式，然后配置全局Jackson null值序列化方式进行使用。

其中：
`自定义注解`：
主要标识某个类、某个字段需要不为null，需要使用自定义序列化方式，然后再根据注解配置进行自定义的序列化

`null值自定义序列化器`：
拦截所有值为null的字段进入自定义序列化器，然后根据`自定义注解`配置进行不同的序列化写值

## 部分实现
1. 自定义注解 `@NeedNotNull`

```java
package com.lth.json.jackson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * json 字段需要不为null
 * 只有字段为null时的序列化才会使用此方式，有值的和正常一样
 * </p>
 *
 * @author Tophua
 * @since 2021/8/13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface NeedNotNull {

    /**
     * 是否排除，只针对字段
     *
     * @return true：按原默认处理，false：按此注解处理
     */
    boolean isExclude() default false;

    /**
     * 自定义值，有此值时已此值为主
     *
     * @return customV
     */
    String customV() default "";

    /**
     * 是否处理Boolean值
     *
     * @return true：Boolean字段为null时序列化为 boolV 值，false：不处理Boolean字段 null -> null
     */
    boolean boolT() default true;

    /**
     * boolean 默认值
     *
     * @return boolV
     */
    boolean boolV() default false;

    /**
     * 是否处理Number值
     *
     * @return true：Number字段为null时序列化为 numberV 值，false：不处理Number字段 null -> null
     */
    boolean numberT() default true;

    /**
     * Number 默认值
     *
     * @return numberV
     */
    int numberV() default 0;

    /**
     * 是否处理String值
     *
     * @return true：String字段为null时序列化为 stringV 值，false：不处理String字段 null -> null
     */
    boolean stringT() default true;

    /**
     * String 默认值
     *
     * @return stringV
     */
    String stringV() default "";

    /**
     * 是否处理Coll（集合和数组）值，null -> 空集合
     *
     * @return true：Coll字段为null时序列化为 '[]' 值，false：不处理Coll字段 null -> null
     */
    boolean collT() default true;

}

```

2. `自定义null值序列化器`

```java
package com.lth.json.jackson;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * <p>
 * json 字段为null时的自定义序列化方式
 * 只有字段为null时的序列化才会使用此方式，有值的和正常一样
 * </p>
 *
 * @author Tophua
 * @since 2021/8/13
 */
@NoArgsConstructor
@AllArgsConstructor
public class NeedNotNullSerialize extends JsonSerializer<Object> {

    private BeanProperty property;

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            gen.writeObject(value);
            return;
        }
        JavaType javaType = property.getType();
        NeedNotNull needNotNull = property.getAnnotation(NeedNotNull.class);
        if (needNotNull == null) {
            needNotNull = property.getContextAnnotation(NeedNotNull.class);
        }
        if (needNotNull == null) {
            gen.writeObject(null);
            return;
        }
        // 是否排除
        if (needNotNull.isExclude()) {
            gen.writeObject(null);
            return;
        }
        // 有自定义值
        if (StrUtil.isNotEmpty(needNotNull.customV())) {
            gen.writeObject(needNotNull.customV());
            return;
        }
        // bool
        if (needNotNull.boolT() && javaType.isTypeOrSubTypeOf(Boolean.class)) {
            gen.writeObject(needNotNull.boolV());
            return;
        }
        // Number
        if (needNotNull.numberT() && javaType.isTypeOrSubTypeOf(Number.class)) {
            gen.writeObject(needNotNull.numberV());
            return;
        }
        // String
        if (needNotNull.stringT() && javaType.isTypeOrSubTypeOf(String.class)) {
            gen.writeObject(needNotNull.stringV());
            return;
        }
        // 集合、数组
        if (needNotNull.collT() && (javaType.isArrayType() || javaType.isCollectionLikeType())) {
            gen.writeObject(ListUtil.empty());
            return;
        }

        gen.writeObject(null);
    }

}

```

3. `Jackson 配置`

```java
package com.lth.json.config;

import cn.hutool.core.date.DatePattern;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.lth.json.jackson.NeedNotNullSerialize;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author skyhua
 */
@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class JacksonConfig {

	@Bean
	public ObjectMapper objectMapper(ApplicationContext applicationContext) {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		builder.applicationContext(applicationContext);
		builder.locale(Locale.CHINA);
		builder.timeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
		builder.simpleDateFormat(DatePattern.NORM_DATETIME_PATTERN);
		builder.modules(new JavaTimeModule());
		ObjectMapper mapper = builder.createXmlMapper(false).build();
		// 为mapper注册一个带有SerializerModifier的Factory，此modifier主要做的事情为：值为null时序列化为默认值
		mapper.setSerializerFactory(mapper.getSerializerFactory().withSerializerModifier(new NeedNotNullSerializerModifier()));
		return mapper;
	}

	/**
	 * <p>
	 * NeedNotNullSerializerModifier 为bean 设置null值序列化器
	 * </p>
	 *
	 * @author Tophua
	 * @since 2021/8/14
	 */
	public static class NeedNotNullSerializerModifier extends BeanSerializerModifier {

		@Override
		public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
			beanProperties.forEach(b -> b.assignNullSerializer(new NeedNotNullSerialize(b)));
			return beanProperties;
		}
	}

	public static class JavaTimeModule extends SimpleModule {
		public JavaTimeModule() {
			super(PackageVersion.VERSION);
			this.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));
			this.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
			this.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
			this.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN)));
			this.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN)));
			this.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN)));
		}
	}
}


```

## 结果演示
默认

```java
@Data
public class Test {
    private Object obj;
    private Boolean bool;
    private Integer intT;
    private Long longT;
    private BigDecimal decimalT;
    private Double doubleT;
    private Float floatT;
    private String stringV;
    private Object[] arrayT;
    private List<Integer> collT;
    private Set<String> setT;
    private String customT;
    private Integer customT1;
    private LocalDateTime now;
    private Test1 test1;

    @Data
    static class Test1 {
        private Integer id;
        private String value;
    }
}
```
默认结果
```json
{
  "obj": null,
  "bool": null,
  "intT": null,
  "longT": null,
  "decimalT": null,
  "doubleT": null,
  "floatT": null,
  "stringV": null,
  "arrayT": null,
  "collT": null,
  "setT": null,
  "customT": null,
  "customT1": null,
  "now": "2021-08-15 17:53:14",
  "test1": {
    "id": null,
    "value": null
  }
}
```

注解用在类上

```java
@Data
@NeedNotNull
public class Test {
    private Object obj;
    private Boolean bool;
    private Integer intT;
    private Long longT;
    private BigDecimal decimalT;
    private Double doubleT;
    private Float floatT;
    private String stringV;
    private Object[] arrayT;
    private List<Integer> collT;
    private Set<String> setT;
    private String customT;
    private Integer customT1;
    private LocalDateTime now;
    private Test1 test1;

    @Data
    static class Test1 {
        private Integer id;
        private String value;
    }
}
```
注解在类上结果

```json
{
  "obj": null,
  "bool": false,
  "intT": 0,
  "longT": 0,
  "decimalT": 0,
  "doubleT": 0,
  "floatT": 0,
  "stringV": "",
  "arrayT": [],
  "collT": [],
  "setT": [],
  "customT": "",
  "customT1": 0,
  "now": "2021-08-15 17:54:00",
  "test1": {
    "id": null,
    "value": null
  }
}
```
可见已有默认值

注解使用在字段上

```java
@Data
public class Test {
    private Object obj;
    private Boolean bool;
    private Integer intT;
    private Long longT;
    private BigDecimal decimalT;
    private Double doubleT;
    private Float floatT;
    @NeedNotNull
    private String stringV;
    private Object[] arrayT;
    @NeedNotNull
    private List<Integer> collT;
    private Set<String> setT;
    private String customT;
    private Integer customT1;
    private LocalDateTime now;
    private Test1 test1;

    @Data
    static class Test1 {
        private Integer id;
        @NeedNotNull
        private String value;
    }
}
```
注解用在字段上结果

```json
{
  "obj": null,
  "bool": null,
  "intT": null,
  "longT": null,
  "decimalT": null,
  "doubleT": null,
  "floatT": null,
  "stringV": "",
  "arrayT": null,
  "collT": [],
  "setT": null,
  "customT": null,
  "customT1": null,
  "now": "2021-08-15 17:58:31",
  "test1": {
    "id": null,
    "value": ""
  }
}
```

花样玩法

```java
@Data
@NeedNotNull
public class Test {
    private Object obj;
    @NeedNotNull(boolT = false)
    private Boolean bool;
    private Integer intT;
    @NeedNotNull(isExclude = true)
    private Long longT;
    private BigDecimal decimalT;
    @NeedNotNull(customV = "0.5")
    private Double doubleT;
    private Float floatT;
    private String stringV;
    @NeedNotNull(customV = "[1,2,3,4]")
    private Object[] arrayT;
    @NeedNotNull(customV = "[1,2,3,4]")
    private List<Integer> collT;
    private Set<String> setT;
    @NeedNotNull(customV = "花样字符串")
    private String customT;
    @NeedNotNull(customV = "100")
    private Integer customT1;
    private LocalDateTime now;
    private Test1 test1;

    @Data
    @NeedNotNull(stringT = false)
    static class Test1 {
        private Integer id;
        private String value;
    }
}
```

花样玩法结果

```json
{
  "obj": null,
  "bool": null,
  "intT": 0,
  "longT": null,
  "decimalT": 0,
  "doubleT": "0.5",
  "floatT": 0,
  "stringV": "",
  "arrayT": "[1,2,3,4]",
  "collT": "[1,2,3,4]",
  "setT": [],
  "customT": "花样字符串",
  "customT1": "100",
  "now": "2021-08-15 18:02:37",
  "test1": {
    "id": 0,
    "value": null
  }
}
```

各式玩法由各位自行去尝试，也可以再进行扩展

## 总结
本人意在解决Jackson序列化时对null值的一些自定义序列化方式，让使用者变的简单，使用方式变的灵活，而不是一杆子打死进行全局配置而忽略一些需要null返回值的场景。
此外，个人觉得在spring项目中还是使用Jackson作为序列化方式比较好，因为这是官方默认的方式，fastjson可以使用，但仅限于代码中做某些json转换（毕竟静态调用还是比较香的）。

我是Tophua，欢迎交流


## 附上源码

[GitHub](https://github.com/TopSkyhua/default-v-demo)

[Gitee](https://gitee.com/TopSkyhua/default-v-demo.git)