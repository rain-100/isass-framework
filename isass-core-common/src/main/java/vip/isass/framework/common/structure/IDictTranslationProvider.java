package vip.isass.framework.common.structure;

/**
 * 字典翻译提供者
 *
 * @author Rain
 * @since 1.0
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
