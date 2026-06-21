package vip.isass.framework.common.support;

public class TestBeanProvider implements BeanProvider {

    @Override
    public Object getContext() {
        return "test-context";
    }
}
