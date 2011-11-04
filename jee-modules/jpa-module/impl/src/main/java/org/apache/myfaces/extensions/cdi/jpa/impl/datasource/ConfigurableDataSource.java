/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.extensions.cdi.jpa.impl.datasource;

import org.apache.myfaces.extensions.cdi.core.api.provider.BeanManagerProvider;
import org.apache.myfaces.extensions.cdi.core.impl.util.JndiUtils;
import org.apache.myfaces.extensions.cdi.jpa.api.datasource.DataSourceConfig;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.logging.Logger;

/**
 * <p>This class can be used instead of a real DataSource.
 * It is a simple wrapper to hide any database configuration details
 * and make it configurable via CDI.</p>
 *
 * <p>The configuration itself will be provided via CDI mechanics.
 * To distinguish different databases, users can specify a
 * <code>connectionId</code>. If no <code>connectionId</code> is set,
 * the String <code>default</code> will be used</p>
 */
public class ConfigurableDataSource implements DataSource
{
    /**
     * config and settings are loaded only once.
     */
    private boolean loaded;

    /**
     * The connectionId allows to configure multiple databases.
     * This can e.g. be used to distinguish between a 'customer' and 'admin'
     * database.
     */
    private String connectionId = "default";

    /**
     * The underlying configuration of the datasource
     */
    private DataSourceConfig dataSourceConfig;

    /**
     *  The underlying 'real' DataSource
     */
    private volatile DataSource wrappedDataSource;

    public ConfigurableDataSource()
    {
        loaded = false;
        dataSourceConfig = BeanManagerProvider.getInstance().getContextualReference(DataSourceConfig.class);
    }

    public void setConnectionId(String connectionId)
    {
        if (loaded)
        {
            throw new IllegalStateException("connectionId must not get changed after the DataSource was established");
        }
        this.connectionId = connectionId;
    }

    public Connection getConnection() throws SQLException
    {
        return getConnection(null, null);
    }

    public Connection getConnection(String userName, String password) throws SQLException
    {
        if (wrappedDataSource == null)
        {
            initDataSource();
        }

        if (userName == null && password == null)
        {
            return wrappedDataSource.getConnection();
        }
        return wrappedDataSource.getConnection(userName, password);
    }


    public PrintWriter getLogWriter() throws SQLException
    {
        return null;
    }

    public void setLogWriter(PrintWriter printWriter) throws SQLException
    {
    }

    public void setLoginTimeout(int loginTimeout) throws SQLException
    {
    }

    public int getLoginTimeout() throws SQLException
    {
        return 0;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException
    {
        if (isWrapperFor(iface))
        {
            return (T) this;
        }
        else
        {
            return null;
        }
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException
    {
        return iface.isAssignableFrom(ConfigurableDataSource.class);
    }

    /**
     * NEW JDK1.7 signature.
     * This makes sure that CODI can also get compiled using java-7.
     * This method is not actively used though.
     */
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     *
     */
    protected synchronized void initDataSource() throws SQLException
    {
        // double check lock idiom on volatile member is ok as of Java5
        if (wrappedDataSource != null)
        {
            return;
        }

        String jndiLookupName = dataSourceConfig.getJndiResourceName(connectionId);
        if (jndiLookupName != null && jndiLookupName.length() > 0)
        {
            wrappedDataSource = JndiUtils.lookup(jndiLookupName, DataSource.class);
            return;
        }

        // no JNDI, so we take the direct JDBC route.
        String jdbcDriverClass = dataSourceConfig.getDriverClassName(connectionId);
        if (jdbcDriverClass == null && jdbcDriverClass.length() == 0)
        {
            throw new SQLException("Neither a JNDI location nor a JDBC driver class name is configured!");
        }

        try
        {
            // we explicitely use class.forName and NOT the ThreadContextClassLoader!
            Class clazz =  Class.forName(jdbcDriverClass);

            // the given driver classname must be a DataSource
            if (!DataSource.class.isAssignableFrom(clazz))
            {
                throw new SQLException("Configured DriverClassName is not a javax.sql.DataSource: "
                                       + jdbcDriverClass);
            }

            wrappedDataSource = (DataSource) clazz.newInstance();

            Map<String, String> config = dataSourceConfig.getConnectionProperties(connectionId);
            for (Map.Entry<String, String> configOption : config.entrySet())
            {
                setProperty(wrappedDataSource, configOption.getKey(), configOption.getValue());
            }
        }
        catch (Exception e)
        {
            wrappedDataSource = null;

            if (e instanceof SQLException)
            {
                throw (SQLException) e;
            }
            throw new SQLException(e);
        }
    }

    private void setProperty(Object instance, String key, String value)
            throws InvocationTargetException, IllegalAccessException
    {
        if (key.length()== 0)
        {
            throw new IllegalArgumentException("property name must not be empty!");
        }

        String setterName = "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        Method setter = null;
        try
        {
            setter = instance.getClass().getMethod(setterName, String.class);
        }
        catch (NoSuchMethodException e)
        {
            try
            {
                setter = instance.getClass().getMethod(setterName, Object.class);
            }
            catch (NoSuchMethodException e1)
            {
                throw new IllegalArgumentException("Cannot find setter with name " + setterName);
            }
        }

        if (!setter.isAccessible())
        {
            setter.setAccessible(true);
        }

        setter.invoke(instance, value);
    }

}
