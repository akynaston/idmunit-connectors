/*
 * IdMUnit - Automated Testing Framework for Identity Management Solutions
 * Copyright (c) 2005-2018 TriVir, LLC
 *
 * This program is licensed under the terms of the GNU General Public License
 * Version 2 (the "License") as published by the Free Software Foundation, and
 * the TriVir Licensing Policies (the "License Policies").  A copy of the License
 * and the Policies were distributed with this program.
 *
 * The License is available at:
 * http://www.gnu.org/copyleft/gpl.html
 *
 * The Policies are available at:
 * http://www.idmunit.org/licensing/index.html
 *
 * Unless required by applicable law or agreed to in writing, this program is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied.  See the License and the Policies
 * for specific language governing the use of this program.
 *
 * www.TriVir.com
 * TriVir LLC
 * 13890 Braddock Road
 * Suite 310
 * Centreville, Virginia 20121
 *
 */

package com.trivir.idmunit.connector;

import org.idmunit.IdMUnitException;
import org.idmunit.connector.AbstractConnector;
import org.idmunit.connector.Connector;
import org.idmunit.connector.ConnectorUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * @author Kenneth Rawlings
 */
public class IterationConnector extends AbstractConnector {

    static final String CONFIG_WRAPPED_CONNECTOR = "wrappedConnector";
    static final String ROW_ITERATION_START = "rowIterationStart";
    static final String ROW_ITERATION_END = "rowIterationEnd";
    static final String ATTRIBUTE_ITERATION_START = "attributeIterationStart";
    static final String ATTRIBUTE_ITERATION_END = "attributeIterationEnd";
    static final String TOKEN_ROW_ITERATION_REGEX = "\\$rowIteration\\$";
    static final String TOKEN_ATTRIBUTE_ITERATION = "$attributeIteration$";
    static final String TOKEN_ATTRIBUTE_ITERATION_REGEX = "\\$attributeIteration\\$";

    Connector wrappedConnector;

    public void setup(Map<String, String> config) throws IdMUnitException {
        Map<String, String> wrappedConnectorConfig = new HashMap<String, String>(config);
        String wrappedConnectorClassName = wrappedConnectorConfig.remove(CONFIG_WRAPPED_CONNECTOR);

        if (wrappedConnectorClassName == null) {
            throw new IdMUnitException(String.format("'%s' is a required setting.", CONFIG_WRAPPED_CONNECTOR));
        }

        Class<? extends Connector> connectorClass;
        try {
            connectorClass = Class.forName(wrappedConnectorClassName).asSubclass(Connector.class);
        } catch (ClassNotFoundException e) {
            throw new IdMUnitException(String.format("Failed to locate class '%s'", wrappedConnectorClassName));
        }

        Constructor<? extends Connector> connectorConstructor;
        try {
            connectorConstructor = connectorClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new IdMUnitException(String.format("Failed to retrieve constructor for class '%s'", wrappedConnectorClassName), e);
        } catch (SecurityException e) {
            throw new IdMUnitException(String.format("Failed to retrieve constructor for class '%s'", wrappedConnectorClassName), e);
        }

        try {
            wrappedConnector = connectorConstructor.newInstance();
        } catch (InstantiationException e) {
            throw new IdMUnitException(String.format("Failed to construct class '%s'", wrappedConnectorClassName), e);
        } catch (IllegalAccessException e) {
            throw new IdMUnitException(String.format("Failed to construct class '%s'", wrappedConnectorClassName), e);
        } catch (IllegalArgumentException e) {
            throw new IdMUnitException(String.format("Failed to construct class '%s'", wrappedConnectorClassName), e);
        } catch (InvocationTargetException e) {
            throw new IdMUnitException(String.format("Failed to construct class '%s'", wrappedConnectorClassName), e);
        }

        wrappedConnector.setup(wrappedConnectorConfig);

    }

    public void tearDown() throws IdMUnitException {
        wrappedConnector.tearDown();
    }

    public void execute(String operation, Map<String, Collection<String>> data) throws IdMUnitException {
        Map<String, Collection<String>> wrappedConnectorData = new HashMap<String, Collection<String>>(data);

        long rowIterationStart = 1;
        try {
            String startValue = ConnectorUtil.getSingleValue(data, ROW_ITERATION_START);
            if (startValue != null) {
                rowIterationStart = Long.parseLong(startValue);
                wrappedConnectorData.remove(ROW_ITERATION_START);
            }
        } catch (NumberFormatException e) {
            throw new IdMUnitException(String.format("'%s' must be a number.", ROW_ITERATION_START));
        }

        long rowIterationEnd = 1;
        try {
            String endValue = ConnectorUtil.getSingleValue(data, ROW_ITERATION_END);
            if (endValue != null) {
                rowIterationEnd = Long.parseLong(endValue);
                wrappedConnectorData.remove(ROW_ITERATION_END);
            }
        } catch (NumberFormatException e) {
            throw new IdMUnitException(String.format("'%s' must be a number.", ROW_ITERATION_END));
        }

        if (rowIterationStart > rowIterationEnd) {
            throw new IdMUnitException(String.format("'%s' must not be greater than '%s'", ROW_ITERATION_START, ROW_ITERATION_END));
        }

        long attributeIterationStart = 1;
        try {
            String startValue = ConnectorUtil.getSingleValue(data, ATTRIBUTE_ITERATION_START);
            if (startValue != null) {
                attributeIterationStart = Long.parseLong(startValue);
                wrappedConnectorData.remove(ATTRIBUTE_ITERATION_START);
            }
        } catch (NumberFormatException e) {
            throw new IdMUnitException(String.format("'%s' must be a number.", ATTRIBUTE_ITERATION_START));
        }

        long attributeIterationEnd = 1;
        try {
            String endValue = ConnectorUtil.getSingleValue(data, ATTRIBUTE_ITERATION_END);
            if (endValue != null) {
                attributeIterationEnd = Long.parseLong(endValue);
                wrappedConnectorData.remove(ATTRIBUTE_ITERATION_END);
            }
        } catch (NumberFormatException e) {
            throw new IdMUnitException(String.format("'%s' must be a number.", ATTRIBUTE_ITERATION_END));
        }

        if (attributeIterationStart > attributeIterationEnd) {
            throw new IdMUnitException(String.format("'%s' must not be greater than '%s'", ATTRIBUTE_ITERATION_START, ATTRIBUTE_ITERATION_END));
        }

        for (long rowIteration = rowIterationStart; rowIteration <= rowIterationEnd; rowIteration++) {
            Map<String, Collection<String>> iterationData = createIterationData(wrappedConnectorData, rowIteration, attributeIterationStart, attributeIterationEnd);
            wrappedConnector.execute(operation, iterationData);
        }

    }

    private Map<String, Collection<String>> createIterationData(Map<String, Collection<String>> data, long rowIteration, long attributeIterationStart, long attributeIterationEnd) throws IdMUnitException {
        Map<String, Collection<String>> iterationData = new HashMap<String, Collection<String>>();

        for (Map.Entry<String, Collection<String>> dataEntry : data.entrySet()) {
            List<String> dataValues = new ArrayList<String>();

            for (String value : dataEntry.getValue()) {
                dataValues.add(value.replaceAll(TOKEN_ROW_ITERATION_REGEX, Long.toString(rowIteration)));
            }

            if (dataEntry.getValue().size() == 1) {
                String value = ConnectorUtil.getSingleValue(data, dataEntry.getKey());
                if (value.indexOf(TOKEN_ATTRIBUTE_ITERATION) >= 0) {
                    dataValues = new ArrayList<String>();
                    for (long attributeIteration = attributeIterationStart; attributeIteration <= attributeIterationEnd; attributeIteration++) {
                        dataValues.add(value.replaceAll(TOKEN_ATTRIBUTE_ITERATION_REGEX, Long.toString(attributeIteration)));
                    }
                }
            }

            iterationData.put(dataEntry.getKey(), dataValues);
        }

        return iterationData;
    }
}
