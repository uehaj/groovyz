/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.codehaus.groovy.ast.*
import org.codehaus.groovy.ast.expr.*
import org.codehaus.groovy.ast.tools.*
import org.codehaus.groovy.control.StaticImportVisitor
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor
import org.codehaus.groovy.transform.stc.TypeCheckingContext

def dictName(dictType) {
    'instance_'+dictType.toString(false).replaceAll(' ', '').replaceAll('\\.', '\\$').replaceAll('[<>]', '_')
}

def fillImplicitParam(MethodCall call, Parameter param, StaticTypeCheckingVisitor typeCheckingVisitor,
                      Map<String, GenericsType> resolvedGenerics) {
    ClassNode dictType = fullyResolveType(param.type, resolvedGenerics)
    String dictName = dictName(dictType)
    println "dictName=$dictName"
    def staticImports = context.enclosingClassNode.module.staticImports
    if (staticImports.containsKey(dictName)) {
        call.arguments.addExpression(new PropertyExpression(new ClassExpression(staticImports[dictName].type), dictName))
    }
    else {
        call.arguments.addExpression(new VariableExpression(dictName, dictType))
    }
}

def resolveTargetGenericTypes(MethodCall call,
                              MethodNode methodNode,
                              TypeCheckingContext context,
                              Map<String, GenericsType> resolvedGenerics) {
    if (context.enclosingBinaryExpression instanceof DeclarationExpression) {
        // TODO: consider Multiple Assignment Declaration
        ClassNode targetDeclaredType = context.enclosingBinaryExpression.variableExpression.type
        GenericsType returnValueOfTheCall = methodNode.returnType.genericsTypes[0]
        println "resolvedGenerics[${returnValueOfTheCall.name}] = new GenericsType(${targetDeclaredType})"
        resolvedGenerics[returnValueOfTheCall.name] = new GenericsType(targetDeclaredType)
        typeCheckingVisitor.storeType(call, targetDeclaredType)
    }
}

def resolveParameterGenericTypesAndFillInplicitParameters(MethodCall call,
                                                          Parameter[] parameters,
                                                          ClassNode[] argumentTypes,
                                                          Map<String, GenericsType> resolvedGenerics) {
    parameters.eachWithIndex { param, idx ->
        if (idx < argumentTypes.size()) {
            inferenceCheck(Collections.EMPTY_SET, resolvedGenerics,
                           param.originType, argumentTypes[idx],
                           idx >= parameters.size() - 1)
        }
        else if (param.hasInitialExpression() && param.initialExpression.text == 'Parameter.IMPLICIT') {
            fillImplicitParam(call, param, typeCheckingVisitor, resolvedGenerics)
        }
    }
}

afterMethodCall { MethodCall call ->
    if (isStaticMethodCallExpression(call)) {
        call.receiver.getMethods(call.method).each { MethodNode methodNode ->
            if (methodNode.genericsTypes != null) {
                Parameter[] parameters = methodNode.parameters;
                ClassNode[] argumentTypes = typeCheckingVisitor.getArgumentTypes(call.arguments)
                Map<String, GenericsType> resolvedGenerics = new HashMap<>();
                resolveTargetGenericTypes(call, methodNode, context, resolvedGenerics)
                resolveParameterGenericTypesAndFillInplicitParameters(call, parameters, argumentTypes, resolvedGenerics)
            }
        }
    }
}
