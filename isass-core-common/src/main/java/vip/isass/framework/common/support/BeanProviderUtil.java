package vip.isass.framework.common.support;

import java.util.Collection;

/**
 * Runtime bean access facade.
 *
 * <p>The actual runtime behavior is supplied by adapters such as
 * {@code isass-adapter-springboot}.</p>
 *
 * @author Rain
 */
public final class BeanProviderUtil {

    private static volatile BeanProvider beanProvider = new BeanProvider() {
    };

    private BeanProviderUtil() {
    }

    public static void setBeanProvider(BeanProvider beanProvider) {
        BeanProviderUtil.beanProvider = beanProvider == null ? new BeanProvider() {
        } : beanProvider;
    }

    public static void setBeanProviderFromServiceLoader() {
        setBeanProvider(IsassServiceLoader.loadFirst(BeanProvider.class).orElse(null));
    }

    public static Object getContext() {
        return beanProvider.getContext();
    }

    /**
     * @return 是否已经初始化运行时环境
     */
    public static boolean isInitialized() {
        return beanProvider.isInitialized();
    }

    /**
     * 创建一个java对象，并添加到运行时容器管理
     *
     * @param <T>       bean class
     * @param beanClass class to new instance
     * @return runtime bean
     */
    public static <T> T addBeanToContext(Class<T> beanClass) {
        return beanProvider.addBean(beanClass);
    }

    /**
     * 根据name获取bean
     *
     * @param name bean name
     * @return bean
     */
    public static Object getBean(String name) {
        return beanProvider.getBean(name);
    }

    /**
     * 根据name获取bean
     *
     * @param <T>          require type
     * @param name         name
     * @param requiredType required type
     * @return bean
     */
    public static <T> T getBean(String name, Class<T> requiredType) {
        return beanProvider.getBean(name, requiredType);
    }

    /**
     * 根据bean类型获取bean
     *
     * @param <T>          require type
     * @param requiredType required type
     * @return bean
     */
    public static <T> T getBean(Class<T> requiredType) {
        return beanProvider.getBean(requiredType);
    }

    public static <T> Collection<T> getBeans(Class<T> requiredType) {
        return beanProvider.getBeans(requiredType);
    }

    public static <T> Collection<T> getBeans(Class<T> requiredType, Object parameterizedTypeReference) {
        return beanProvider.getBeans(requiredType);
    }

    public static <T, P> T getBean(Class<T> requiredType, Class<P> type) {
        return beanProvider.getBean(requiredType, type);
    }

    public static <T, P> T getBeanOfSupport(Class<T> requiredType, Class<P> supportType) {
        Collection<T> beans = beanProvider.getBeans(requiredType);
        for (T bean : beans) {
            if (!(bean instanceof Support support)) {
                continue;
            }
            if (support.support(supportType)) {
                return bean;
            }
        }
        return null;
    }

    /**
     * 根据bean类型，构造器参数获取bean
     *
     * @param <T>          require type
     * @param requiredType required type
     * @param objects      objects
     * @return bean
     */
    public static <T> T getBean(Class<T> requiredType, Object... objects) {
        return beanProvider.getBean(requiredType, objects);
    }

    /**
     * 根据类名获取bean
     *
     * @param <T>       require type
     * @param beanClass bean class
     * @return bean
     */
    public static <T> T getBeanByNameOfBeanType(Class<T> beanClass) {
        String beanName = getBeanNameByBeanType(beanClass);
        @SuppressWarnings("unchecked")
        T bean = (T) beanProvider.getBean(beanName);
        return bean;
    }

    /**
     * 根据非限定类名获取bean的名称，即将类名的首字母小写
     *
     * @param beanClass bean class
     * @return bean name
     */
    public static String getBeanNameByBeanType(Class<?> beanClass) {
        String beanName = beanClass.getSimpleName();
        return beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
    }

    /**
     * 移除 bean
     *
     * @param beanName bean name
     */
    public static void unRegistryBean(String beanName) {
        beanProvider.unRegistryBean(beanName);
    }

    /**
     * 移除 bean
     *
     * @param beanClass bean class
     */
    public static void unRegistryBean(Class<?> beanClass) {
        beanProvider.unRegistryBean(beanClass);
    }

    /**
     * 获取实现某接口的所有实例 bean 名称
     *
     * @param type 接口
     * @return beanName 数组
     */
    public static String[] getBeanNamesForType(Class<?> type) {
        return beanProvider.getBeanNamesForType(type);
    }
}
