//package vip.isass.auth;
//
//import com.baomidou.mybatisplus.core.metadata.IPage;
//import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
//import feign.MethodMetadata;
//import org.apache.commons.lang3.reflect.TypeUtils;
//import org.springframework.cloud.openfeign.support.SpringMvcContract;
//import org.springframework.context.annotation.Configuration;
//
//import java.lang.reflect.Method;
//import java.lang.reflect.ParameterizedType;
//import java.lang.reflect.Type;
//
/**
 * 可以改写 feign 的返回值类型，例如把返回值是接口改为实现类
 */
//@Configuration
//public class WebContractConfig extends SpringMvcContract {
//
//    @Override
//    public MethodMetadata parseAndValidateMetadata(Class<?> targetType, Method method) {
//        MethodMetadata methodMetadata = super.parseAndValidateMetadata(targetType, method);
//        Type returnType = methodMetadata.returnType();
//        if (returnType instanceof ParameterizedType
//                && ((ParameterizedType) returnType).getRawType() == IPage.class) {
//            methodMetadata.returnType(
//                    TypeUtils.parameterizeWithOwner(
//                            ((ParameterizedType) returnType).getOwnerType(),
//                            Page.class,
//                            ((ParameterizedType) returnType).getActualTypeArguments())
//            );
//        }
//        return methodMetadata;
//    }
//}
