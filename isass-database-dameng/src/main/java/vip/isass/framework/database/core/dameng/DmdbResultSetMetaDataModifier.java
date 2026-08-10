// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.core.dameng;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;
import javassist.NotFoundException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DmdbResultSetMetaDataModifier {

    static {
        DmdbResultSetMetaDataModifier.init();
    }

    public static void init() {
        try {
            Class.forName("dm.jdbc.driver.DmdbResultSetMetaData");

            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
            CtClass ctClass = pool.get("dm.jdbc.driver.DmdbResultSetMetaData");
            addDoGetColumnNameMethod(ctClass);
            ctClass.toClass();
            ctClass.detach();
        } catch (ClassNotFoundException e) {
            // 如果没有这个类，说明不是 dameng 数据库，不需要修改
        } catch (NotFoundException | CannotCompileException e) {
            // 如果找不到类或编译异常，记录日志
            Logger.getLogger(DmdbResultSetMetaDataModifier.class.getName())
                    .log(Level.SEVERE, "can not modify source code of class 'dm.jdbc.driver.DmdbResultSetMetaData'", e);
        }
    }

    /**
     * 改写 do_getColumnName 方法。
     * <p>
     * baseName 是原字段名，name 是 sql 写的 as 别名
     * <br>
     * 旧的驱动，只拿 name，新的会拿 baseName，sql 都给了别名了，还拿 baseName
     */
    private static void addDoGetColumnNameMethod(CtClass ctClass) {
        try {
            String methodStr = " " +
                    "public String do_getColumnName(int var1) {" +
                    "    Column var2 = this.checkIndex($1);" +
                    "    String var3 = var2.name;" +
                    "    if (var3 == null) {" +
                    "        return var3;" +
                    "    } else if (this.connection.isColumnNameUpperCase()) {" +
                    "        return var3.toUpperCase();" +
                    "    } else {" +
                    "        return this.connection.isColumnNameLowerCase() ? var3.toLowerCase() : var3;" +
                    "    }" +
                    "}";
            CtMethod method = CtNewMethod.make(methodStr, ctClass);
            ctClass.addMethod(method);
        } catch (CannotCompileException e) {
            Logger.getLogger(DmdbResultSetMetaDataModifier.class.getName())
                    .log(Level.SEVERE, "can not modify source code of class 'dm.jdbc.driver.DmdbResultSetMetaData'", e);
        }
    }

}
