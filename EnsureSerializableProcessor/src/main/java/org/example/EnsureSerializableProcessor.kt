package org.example

import com.google.auto.service.AutoService
import java.io.Serializable
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import javax.tools.Diagnostic


@AutoService(Processor::class)
@SupportedAnnotationTypes("org.example.EnsureSerializable")
class EnsureSerializableProcessor : AbstractProcessor() {
    private val visitedNodes = mutableSetOf<String>()

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        val symbols = roundEnv
            ?.getElementsAnnotatedWith(EnsureSerializable::class.java)
            ?.filter { it.kind == ElementKind.CLASS }
            ?.filterIsInstance<TypeElement>()
            ?: emptyList()
        symbols.forEach { node ->
            if (!implementsSerializableInterface(node)) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    rootErrorMessage(node)
                )
            } else {
                processRecursively(node)
            }
        }
        return true
    }

    private fun processRecursively(node: TypeElement) {
        val nodeName = node.qualifiedName!!.toString()
        if (isNotVisited(nodeName)) markAsVisited(nodeName) else return

        val childNodes = node.enclosedElements
            ?.filter { it.kind == ElementKind.FIELD }
            ?.mapNotNull {
                val className = it.asType().toString()
                processingEnv.elementUtils.getTypeElement(className)
            }
            ?: emptyList()

        for (childNode in childNodes) {
            if (!implementsSerializableInterface(childNode)) {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    childErrorMessage(node, childNode)
                )
            } else {
                processRecursively(childNode)
            }
        }
    }

    private fun rootErrorMessage(node: TypeElement): String {
        return "Class \"${node.simpleName}\" is not serializable"
    }

    private fun childErrorMessage(node: TypeElement, childNode: TypeElement): String {
        return "Class \"${node.simpleName}\" has not serializable child of type \"${childNode.simpleName}\""
    }

    private fun isNotVisited(nodeName: String): Boolean {
        return !visitedNodes.contains(nodeName)
    }

    private fun markAsVisited(nodeName: String) {
        visitedNodes.add(nodeName)
    }

    private fun implementsSerializableInterface(node: TypeElement): Boolean {
        val serializable: TypeMirror =
            processingEnv.elementUtils.getTypeElement(Serializable::class.java.canonicalName).asType()
        return processingEnv.typeUtils.isAssignable(node.asType(), serializable)
    }

}