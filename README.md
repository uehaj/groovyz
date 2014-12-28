groovyz
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

* interface Monoid<T> {
* interface Functor<F> {
* interface Applicative<A> extends Functor<A> {
* interface Monad<M> extends Applicative<M> {
* interface Show<T> {
* trait Eq<T> { // One of eq or neq, or both should be overriden.
* trait Ord<T> extends Eq<T> {

Sampl Type Class Instances
-----------------------------------

* class OptionalMonoid<T> implements Monoid<Optional<T>> {
* class ListFunctor implements Functor<List> {
* class OptionalFunctor implements Functor<Optional> {
* class ListApplicative extends ListFunctor implements Applicative<List> {
* class ListMonad extends ListApplicative implements Monad<List> {
* class IntShow implements Show<Integer> {
* class StringShow implements Show<String> {
* class ListShow<T> implements Show<List<T>> {
* class IntEq implements Eq<Integer> {
* class IntOrd implements Ord<Integer> {


Why I write this?
==================

Type class is a language constract which available langages Haskell, Scala and Rust and so on.
As my personal impression, it was difficult to understand.
So I'd like to explain through implementating it on super self-customizable langage, Groovy.

Note: This code implement only the core concept of Type class, and details should be different in real other langages.

What is Type Class?
=====================

Type class enables us to restrict Genrics(polimorphic) type parameters to what operations can be done on it.
It also give the guarantee what operation can be done when using the type parameters in function which takes the type parameter as a type of it's parameter.

From another point of view, type class provides a way to restrict and guarantee type parameter **without using class inheritance**.

For example, following code using method generics in Groovy,

```
static<T> void runSomething(T a) {
   a.run()
}
```

when we want to restrict the type parameter T, we can specify implement Runnable

```
static<T implements Runnable> void runSomething(T a) {
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
* (2) Instanciate that type class
* (3) Define a function which takes generic type parameters and restrict the type parameter with type class.
* (4) Call the function with type class instance which selected automaticall from the scope.


(1) Define a type class
-----------------------------

Define Monoid type class as an interface.

```
@TypeChecked
interface Monoid<T> {
    def T mappend(T t1, T t2);
    def T mempty();
}
```

Use 'def T' instead of just 'T' because of syntax restriction.

You can see this is a normal Interface. You can inherit from this to define another type class.
Probably you might be confused because we use class inheritance here. It doesn't be avoiding class inheritance.

We only use class here for just a place to put method.
If we define full AST fransformation for this, methods can be static and search method along with inheritance relation in original way.
To sum up, to use inheritance and instance method is a kind of quick hack. In Haskell they are using static function, and in scala, they are using method of singleton(object).

To sum up,

* Define set of operations as methods. this is used after as restriction of Generics parameter(=type class)
* Defined Methods can call each other.
* Method body can be defined if we needed. In those case use trait of Groovy.

(2) Instanciate that type class
-----------------------------

Instanciation of type class is, to declare an type that it saticefies a type class restrictions, and fill up abstract methods body.

Following code declares instantiate String as a Monoid type class.

```
def instance_Monoid_java$lang$String_ = new Monoid<String>() {
    @Override String mappend(String i1, String i2) {
        i1+i2
    }
    @Override String mempty() {
        ""
    }
}
```
The name of the variable which holds a type class instance, in this case instance_Monoid_java$lang$String_', should be follow on some naming convention. The reason why this is to make Groovy to search type class instance on scope. This will be explaind in step (4).

To here we use only plain normal Groovy code. There is nothing special thing.

(3) Define a function which takes generic type parameters and restrict the type parameter with type class.
-----------------------------

Now, let's define generic function which takes a generic parameter on which the type class restriction applied:

```
@TypeChecked
class Functions {
    static <T> T mappend(T t1, T t2, Monoid<T> dict=Parameter.IMPLICIT) { dict.mappend(t1,t2) }
    static <T> T mempty(Monoid<T> dict=Parameter.IMPLICIT) { dict.mempty() }
}
```

The parameter "Monoid dict=Parameter.IMPLICIT" is a declaration of the type class restrictions in this impementation, and the meaning in here is:
T of returnd value and the parameter t1, t2 should be satisfied Monoid type class restriction.
You might think it's verbose that call to mappend calls same name method mappend.
In haskell, functions on type class instance is already static, but this implementation uses functions as instancemethod, this step to expose to static space is required.

Passed parameter dict is used as namespace of functions. You call dispatch any method call on the namespece. You can use Groovy's with phrase in here like:

```
    .... {
       dict.with { mappend(t1,t2) }
    }
```


(4) Call the function with type class instance which selected automaticall from the scope.
-----------------------------

Filally, call defined defined generic functions using Type Class.

```
import static Functions.*
 :
@TypeChecked(extensions='ImplicitParamTransformer.groovy')
  :
        String s = mempty()
        assert s == ""
        assert mappend("a","b") == "ab"
```

This code is can be run unser STC(Static Type Chekcing).

Arguments for parameter Monoid type instance is need not to appeal but it is choosed and supplied inplicitly by InplicitParamTransformar custom type checker(type checking extension). ImplicitParamTramsformer does following:

* if a static method call uses generics,
* check the definition of the static method.
* and if the method have a parameter which default initial value is Paraemter.IMPLICIT
* resolve generics generics paramter types and return type.
   * do simple amd limited target type inference
* apply resolved generics types to actural type of paramter which takes Parameter.IMPLICIT initial value.
* determine actural type of paramter which takes Parameter.IMPLICIT
* encode the type to variable name
* modify AST to provide the variable which has the name, as the parameter which takes Parameter.IPLICIT intial value

For example, when calling 'mappend("a", "b")', correspond static method definition which have the name "mappend" is:

```
    static <T> T mappend(T t1, T t2, Monoid<T> dict=Parameter.IMPLICIT) { dict.mappend(t1,t2) }
```

So infer the generics type T(for return type and parameter type of t1,t2) is String, and Monoid<T> realize it to be Monoid&ltString>, so encode to variable name instand_Monoid_java$lang$String. This call is finally converted to:

```
mappend("a","b",instance_Monoid_java$lang$String_ )
```

Even if the function takses no paramter, we can infer by target type inference so "String s=empty()" can be converted to:

```
String s = mempty(instance_Monoid_java$lang$String_ )
```

In Type class of Scala, it searchs by type and ignore variable name. But this implemention doesn't do it. Instead leave it to Groovy's variable scope.


Type constorctor as type paramter(Higher-kind Generics)
----------------------------------------------------------------------------

Gorovy generics is a different from Java. In Java genrics, generics type parameter type can't be a type parameter which takes type parameter.
In the other words, type constorctor can't be pass as type paramter. But Groovy can do it.
For example, 'List' of List&lt;T> or 'Optional' of Optional&<T> can't be pass as generics paramter in Java but Groovy can.

We actually use this feature in this library like follwing:

```
@TypeChecked
interface Applicative<A> extends Functor<A> {
    public <T> A<T> pure(T t)
    public <T,R> A<R> ap(A<Function<T,R>> func, A<T> a) // <*> :: f (a -> b) -> f a -> f b 
}
```

This type of generics is especially vital to represent highly abstract type like Monad, so on.

TODOs
=========

* type class instance name auto generation.
* get genrics parameter information of static function other than import static declaration(it's fatal for modulalization!)


Summary
===============

Groovy's custum type checker is very powerful technology not only for type checkeing but also implemnet a kind of language fueture like Type Class.
But it moght be abuse of for 'type checker'. I hope general feture similer to custom type checker but can be specify compile phase and more AST node match events.

Enjoy!
