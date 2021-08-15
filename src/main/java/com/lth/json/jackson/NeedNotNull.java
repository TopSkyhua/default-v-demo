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
