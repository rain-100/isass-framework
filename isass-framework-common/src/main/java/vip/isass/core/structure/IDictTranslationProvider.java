package vip.isass.core.structure;

/**
 * @author : rain
 * @date : 2023/3/30
 */
public interface IDictTranslationProvider {

    /**
     * 翻译字典
     *
     * @param typeCode   字典类型典编码
     * @param optionCode 字典选项编码
     * @return 字典名称
     */
    String translate(String typeCode, String optionCode);

}
