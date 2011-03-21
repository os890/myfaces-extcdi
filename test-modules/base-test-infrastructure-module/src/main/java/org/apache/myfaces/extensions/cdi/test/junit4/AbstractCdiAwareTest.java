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
package org.apache.myfaces.extensions.cdi.test.junit4;

import org.apache.myfaces.extensions.cdi.test.TestContainerFactory;
import org.apache.myfaces.extensions.cdi.test.spi.CdiTestContainer;
import org.junit.After;
import org.junit.Before;

/**
 * Allows dependency injection in (standalone) JUnit tests.
 *
 * @author Gerhard Petracek
 */
public abstract class AbstractCdiAwareTest
{
    protected CdiTestContainer testContainer;

    @Before
    public void before() throws Exception
    {
        this.testContainer = TestContainerFactory.getNewCdiTestContainer(false);
        this.testContainer.initEnvironment();
        this.testContainer.startContainer();
        this.testContainer.startContexts();

        this.testContainer.injectFields(this);
    }

    @After
    public void after() throws Exception
    {
        this.testContainer.stopContexts();
        this.testContainer.startContainer();
    }
}
