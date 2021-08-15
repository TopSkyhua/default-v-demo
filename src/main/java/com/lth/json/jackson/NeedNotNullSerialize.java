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
