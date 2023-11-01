package test

import org.example.EnsureSerializable
import java.io.Serializable

@EnsureSerializable
data class GrandParent constructor(
    val a: String,
    val b: Parent,
) : Serializable

data class Parent(
    val a: Child1,
    val b: Child2,
    val c: Child3,
) : Serializable, Cloneable

class Child1 : Serializable
class Child2 : Serializable

open class SuperChild3 : Serializable
class Child3(val a: Child3?) : SuperChild3()


