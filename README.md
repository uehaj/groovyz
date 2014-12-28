Groovyz
=======

POC of Type Class for Groovy.

How to Run
==========

```
  groovy sample.groovy
```

Groovy 2.4.x or higher is recommended.

Providing type class library
===============================

Type classes
-------------------------

* interface Monoid&lt;T> {
* interface Functor&lt;F> {
* interface Applicative&lt;A> extends Functor&lt;A> {
* interface Monad&lt;M> extends Applicative&lt;M> {
* interface Show&lt;T> {
* trait Eq&lt;T> { // One of eq or neq, or both should be overridden.
* trait Ord&lt;T> extends Eq&lt;T> {

Sample Type Class Instances
-----------------------------------

* class OptionalMonoid&lt;T> implements Monoid&lt;Optional&lt;T>> {
* class ListFunctor implements Functor&lt;List> {
* class OptionalFunctor implements Functor&lt;Optional> {
* class ListApplicative extends ListFunctor implements Applicative&lt;List> {
* class ListMonad extends ListApplicative implements Monad&lt;List> {
* class IntShow implements Show&lt;Integer> {
* class StringShow implements Show&lt;String> {
* class ListShow&lt;T> implements Show&lt;List&lt;T>> {
* class IntEq implements Eq&lt;Integer> {
* class IntOrd implements Ord&lt;Integer> {


Why I write this?
==================

Type class is a language construct which available languages Haskell, Scala and Rust and so on.
As my personal impression, it was difficult to understand.
So I'd like to explain through implementing it on super self-customizable language, Groovy.

Note: This code implement only the core concept of Type class, and details should be different in real other languages.

What is Type Class?
=====================

Type class enables us to restrict Generic(polymorphic) type parameters to what operations can be done on it.
It also give the guarantee what operation can be done when using the type parameters in function which takes the type parameter as a type of its parameter.

From another point of view, type class provides a way to restrict and guarantee type parameter **without using class inheritance**.

For example, following code using method generics in Groovy,

```
static&lt;T> void runSomething(T a) {
   a.run()
}
```

When we want to restrict the type parameter T, we can specify implement Runnable

```
static&lt;T implements Runnable> void runSomething(T a) {
   a.run()
}
```

By using class inheritance, we can restrict and guarantee that method can be called to parameter T a.
This is not bad, but the motivation of introduce type class is avoiding class inheritance.

Why avoiding class inheritance for generics parameter?
==============================================================

[TBD]

4 steps of applying type class
=================================================

We'd like to explain mechanism of our Groovy type class with following 4 steps:

* (1) Define a type class
* (2) Instantiate that type class
* (3) Define a function that takes generic type parameters and restrict the type parameter with type class.
* (4) Call the function with type class instance that selected automatically from the scope.


(1) Define a type class
-----------------------------

Define Monoid&lt;T> type class as an interface.

```
@TypeChecked
interface Monoid&lt;T> {
    def T mappend(T t1, T t2);
    def T mempty();
}
```

Use 'def T' instead of just 'T' because of syntax restriction.

You can see this is a normal Interface. You can inherit from this to define another type class.
Probably you might be confused because we use class inheritance here. It doesn't be avoiding class inheritance.

We only use class here for just a place to put method. And ever use instance variable. If we define full AST transformation for this, methods can be static and search method along with inheritance relation in original way.
To sum up, to use inheritance and instance method is a kind of quick hack. In Haskell they are using static function, and in scala, they are using method of singleton (object).

To sum up,

* Define set of operations as methods of class. This is class is used as restriction of Generics parameter (=type class). This class and derived class never use instance variable.
* Defined Methods can call each other, but those are never use instance variable.
* Method body can be defined if we needed. You can use trait to define method body.

(2) Instantiate that type class
-----------------------------

Instantiation of type class is, to declare a type that it satisfies a type class restrictions, and fill up abstract methods body.

Following code declares instantiate String as a Monoid type class.

```
def instance_Monoid_java$lang$String_ = new Monoid&lt;String>() {
    @Override String mappend(String i1, String i2) {
        i1+i2
    }
    @Override String mempty() {
        ""
    }
}
```
The name of the variable which holds a type class instance, in this case instance_Monoid_java$lang$String_', should be follow on some naming convention. Groovy’ s scope handling resolves type class instance. See step (4).

Until here we use only plain normal Groovy code. There is nothing-special thing.

(3) Define a function, which takes generic type parameters and restrict the type parameter with type class.
-----------------------------

Now, let's define generic function which takes a generic parameter on which the type class restriction applied:

```
@TypeChecked
class Functions {
    static &lt;T> T mappend(T t1, T t2, Monoid&lt;T> dict=Parameter.IMPLICIT) { dict.mappend(t1,t2) }
    static &lt;T> T mempty(Monoid&lt;T> dict=Parameter.IMPLICIT) { dict.mempty() }
}
```

The parameter "Monoid dict=Parameter.IMPLICIT" is a declaration of the type class restrictions in this implementation, and the meaning in here is:
T of returned value and the parameter t1, t2 should be satisfied Monoid type class restriction.
You might think it's verbose that call to mappend calls same name method mappend.
In Haskell, functions on type class instance are already static, but this implementation uses functions as instance method, this step means to expose to static space.

Passed parameter dict is used as namespace of functions. You call dispatch any method call on the namespace. You can use Groovy' s with phrase in here like:

```
    .... {
       dict.with { mappend(t1,t2) }
    }
```


(4) Call the function with type class instance, which selected automatically from the scope.
-----------------------------

Finally, call defined generic functions using Type Class.

```
import static Functions.*
 :
@TypeChecked(extensions='ImplicitParamTransformer.groovy')
  :
        String s = mempty()
        assert s == ""
        assert mappend("a","b") == "ab"
```

This code is can be run under STC(Static Type Checking).

Arguments for parameter Monoid type instance is need not to appeal but it is choose and supplied implicitly by InplicitParamTransformar custom type checker(type checking extension). ImplicitParamTramsformer does following:

* If a static method call uses generics,
* Check the definition of the static method.
* And if the method have a parameter which default initial value is Paraemter.IMPLICIT
* Resolve generics paramter types and return type.
   * Do simple and limited target type inference
* Apply resolved generics types to actual type of parameter which takes Parameter.IMPLICIT initial value.
* Determine actual type of parameter, which takes Parameter.IMPLICIT
* Encode the type to variable name
* Modify AST to provide a variable, which has the type-encoded name, as the parameter that takes Parameter.IPLICIT initial value

For example, when calling 'mappend("a", "b")', correspond static method definition which have the name "mappend" is:

```
    static &lt;T> T mappend(T t1, T t2, Monoid&lt;T> dict=Parameter.IMPLICIT) { dict.mappend(t1,t2) }
```

So infer the generics type T(for return type and parameter type of t1,t2) is String, and Monoid&lt;T> realize it to be Monoid&lt;String>, so encode to variable name `instand_Monoid_java$lang$String`. This call is finally converted to:

```
mappend("a","b",instance_Monoid_java$lang$String_ )
```

Even if the function takes no parameter, we can infer by target type inference so "String s=empty()" would be converted to:

```
String s = mempty(instance_Monoid_java$lang$String_ )
```

In Type class of Scala, it searches by type and ignore variable name. But this implementation doesn't do it. Instead leave it to Groovy's variable scope.


Type constructor as type parameter(Higher-kind Generics)
----------------------------------------------------------------------------

Groovy generics is a different from Java. In Java generics, generics type parameter type can't be a type parameter which takes type parameter.
In the other words, type constructor can't be pass as type parameter. But Groovy can do it.
For example, 'List' of List&lt;T> or 'Optional' of Optional&lt;T> can't be pass as generics parameter in Java but Groovy can.

We actually use this feature in this type class sample library like following:

```
@TypeChecked
interface Applicative&lt;A> extends Functor&lt;A> {
    public &lt;T> A&lt;T> pure(T t)
    public &lt;T,R> A&lt;R> ap(A&lt;Function&lt;T,R>> func, A&lt;T> a) // &lt;*> :: f (a -> b) -> f a -> f b 
}
```

This type of generics is especially vital to represent highly abstract type like Monad, so on.

TODOs
=========

* Type class instance name auto generation.
* Get generics parameter / return value information of static function other than import static declaration (it's fatal for modularization!)


Summary
===============

Groovy's custom type checker is very powerful technology not only for type checking but also implement a kind of language feature like Type Class.
But it might be abuse of for 'type checker'. I hope a new feature introduced to Groovy, which is general feature similar to custom type checker but can be specified various compile phase and more AST node match events.

Enjoy!

