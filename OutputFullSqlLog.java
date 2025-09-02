


import lombok.RequiredArgsConstructor;
        import org.apache.ibatis.executor.statement.StatementHandler;
        import org.apache.ibatis.mapping.BoundSql;
        import org.apache.ibatis.mapping.MappedStatement;
        import org.apache.ibatis.mapping.ParameterMapping;
        import org.apache.ibatis.plugin.Intercepts;
        import org.apache.ibatis.plugin.Plugin;
        import org.apache.ibatis.plugin.Signature;
        import org.apache.ibatis.reflection.DefaultReflectorFactory;
        import org.apache.ibatis.reflection.MetaObject;
        import org.apache.ibatis.reflection.SystemMetaObject;
        import org.apache.ibatis.session.Configuration;
        import org.apache.ibatis.type.TypeHandlerRegistry;
        import org.springframework.beans.factory.annotation.Value;
        import org.springframework.stereotype.Component;


        import java.text.DateFormat;
        import java.util.List;
        import java.util.Locale;
        import java.util.regex.Matcher;


        import java.sql.Connection;
        import java.util.Date;
        import java.util.Properties;


        import lombok.extern.slf4j.Slf4j;
        import org.apache.commons.collections4.CollectionUtils;
        import org.apache.ibatis.plugin.Interceptor;
        import org.apache.ibatis.plugin.Invocation;


/**
 * Class       MybatisPlusLogInterceptor
 * date        2024/4/24 9:57
 * Email       liangchen_beijing@163.com
 * Description   mybatis 的拦截器，功能：打印完整的sql。仅在nacos中配置了 showFullSql=true 的时候生效。若nacos没有配置也不会报错，默认值为false
 * <p/>
 * 打印示例：
 * <p/>
 * 2024-04-24 10:13:13.397|DEBUG|http-nio-8083-exec-2|.interceptor.MybatisPlusLogInterceptor-intercept:65 > id ==> selectPage_mpCount
 * <p/>
 * 2024-04-24 10:13:13.398|DEBUG|http-nio-8083-exec-2|.interceptor.MybatisPlusLogInterceptor-intercept:68 > 类型 ==> SELECT
 * <p/>
 * 2024-04-24 10:13:13.776|DEBUG|http-nio-8083-exec-2|interceptor.MybatisPlusLogInterceptor-intercept:76 > 拦截的sql ==>: selectPage_mpCount:SELECT COUNT(1) FROM xxx WHERE has_del = 0 AND (xx = 'xx' AND (((xx IN ('003002', '004001', '001902006', '001905010')))
 */


@Slf4j
@Component
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class MybatisPlusLogInterceptor implements Interceptor {


    @Value("${showFullSql:false}")
    private Boolean showFullSql;




    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (showFullSql) {

            StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
            //通过MetaObject优雅访问对象的属性，这里是访问statementHandler的属性;：MetaObject是Mybatis提供的一个用于方便、
            //优雅访问对象属性的对象，通过它可以简化代码、不需要try/catch各种reflect异常，同时它支持对JavaBean、Collection、Map三种类型对象的操作。
            MetaObject metaObject = MetaObject

                    .forObject(statementHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
                            new DefaultReflectorFactory());
            //先拦截到RoutingStatementHandler，里面有个StatementHandler类型的delegate变量，其实现类是BaseStatementHandler，然后就到BaseStatementHandler的成员变量mappedStatement
            MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
            //id为执行的mapper方法的全路径名，如com.uv.dao.UserMapper.insertUser
            String id = mappedStatement.getId();
            log.debug("id ==> " + id);
            //sql语句类型 select、delete、insert、update
            String sqlCommandType = mappedStatement.getSqlCommandType().toString();
            log.debug("类型 ==> " + sqlCommandType);


            BoundSql boundSql = statementHandler.getBoundSql();


            // 获取节点的配置
            Configuration configuration = mappedStatement.getConfiguration();
            // 获取到最终的sql语句
            String newsql = getSql(configuration, boundSql, id);
            log.debug("拦截的sql ==>: " + newsql);
            long start = System.currentTimeMillis();
            Object returnValue = invocation.proceed();
            long end = System.currentTimeMillis();
            long time = (end - start);
            log.debug("sql耗时(毫秒) ==>: " + time);
            return returnValue;
        }
        return invocation.proceed();
    }


    @Override
    public Object plugin(Object target) {
        if (showFullSql) {
            return Plugin.wrap(target, this);
        }
        return target;
    }


    @Override
    public void setProperties(Properties properties) {}
    /**
     * 封装了一下sql语句，
     * 使得结果返回完整xml路径下的sql语句节点id + sql语句
     *
     * @param configuration
     * @param boundSql
     * @param sqlId
     * @return
     */
    private String getSql(Configuration configuration, BoundSql boundSql, String sqlId) {

        String sql = showSql(configuration, boundSql);
        StringBuilder str = new StringBuilder(100);
        str.append(sqlId);
        str.append(":");
        str.append(sql);
        return str.toString();
    }
    /**
     * 如果参数是String，则添加单引号， 如果是日期，则转换为时间格式器并加单引号；
     * 对参数是null和不是null的情况作了处理

     *
     * @param obj
     * @return
     */
    private String getParameterValue(Object obj) {

        String value = null;
        if (obj instanceof String) {

            value = "'" + obj.toString() + "'";
        } else if (obj instanceof Date) {

            DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.CHINA);
            value = "'" + formatter.format(new Date()) + "'";
        } else {
            if (obj != null) {

                value = obj.toString();
            } else {

                value = "";
            }
        }
        return value;
    }
    /**
     * 进行？的替换
     *
     * @param configuration
     * @param boundSql
     * @return
     */
    public String showSql(Configuration configuration, BoundSql boundSql) {
        // 获取参数
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql

                .getParameterMappings();
        // sql语句中多个空格都用一个空格代替
        String sql = boundSql.getSql().replaceAll("[\\s]+",
                " ");
        if (CollectionUtils.isNotEmpty(parameterMappings) && parameterObject != null) {
            // 获取类型处理器注册器，类型处理器的功能是进行java类型和数据库类型的转换　　　　　　　
            // 如果根据parameterObject.getClass(）可以找到对应的类型，则替换
            TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
            if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {

                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(parameterObject)));
            } else {
                //MetaObject主要是封装了originalObject对象，
                // 提供了get和set的方法用于获取和设置originalObject的属性值,
                // 主要支持对JavaBean、Collection、Map三种类型对象的操作
                MetaObject metaObject = configuration.newMetaObject(parameterObject);
                for (ParameterMapping parameterMapping : parameterMappings) {

                    String propertyName = parameterMapping.getProperty();
                    if (metaObject.hasGetter(propertyName)) {

                        Object obj = metaObject.getValue(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        // 该分支是动态sql
                        Object obj = boundSql.getAdditionalParameter(propertyName);
                        sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(getParameterValue(obj)));
                    } else {
                        //打印出缺失，提醒该参数缺失并防止错位
                        sql = sql.replaceFirst("\\?",
                                "缺失");
                    }
                }
            }
        }
        return sql;
    }
}
