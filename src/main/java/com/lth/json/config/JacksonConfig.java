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
