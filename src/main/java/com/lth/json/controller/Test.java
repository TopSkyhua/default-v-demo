package com.lth.json.controller;

import com.lth.json.jackson.NeedNotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * test
 * </p>
 *
 * @author Tophua
 * @since 2021/8/14
 */
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
