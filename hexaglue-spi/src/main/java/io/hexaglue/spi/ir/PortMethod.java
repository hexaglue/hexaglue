package io.hexaglue.spi.ir;

import java.util.List;

/**
 * A method declared on a port interface.
 *
 * @param name the method name
 * @param returnType the return type (qualified name)
 * @param parameters the parameter types (qualified names)
 */
public record PortMethod(String name, String returnType, List<String> parameters) {}
