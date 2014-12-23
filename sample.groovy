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

import java.util.function.*
import groovy.transform.TypeChecked
import static Functions.*

class Parameter {
    static final Object IMPLICIT = null
}

@TypeChecked
interface Monoid<T> {
    def T mappend(T t1, T t2);
    def T mempty();
}

@TypeChecked
class OptionalMonoid<T> implements Monoid<Optional<T>> {
    Monoid<T> elemMonoidDict
    OptionalMonoid(Monoid<T> monoidDict=null) {
        this.elemMonoidDict = monoidDict
    }
    @Override
    public Optional<T> mappend(Optional<T> i1, Optional<T> i2) {
        if (i1.isPresent() && !i2.isPresent()) {
            return i1
        }
        else if (!i1.isPresent() && i2.isPresent()) {
            return i2
        }
        else if (!i1.isPresent() && !i2.isPresent()) {
            return Optional.empty()
        }
        return Optional.of(elemMonoidDict.mappend(i1.get(), i2.get()))
    }
    @Override
    public Optional<T> mempty() {
        Optional.empty()
    }
}

@TypeChecked
interface Functor<F> {
    public <T,R> F<R> fmap(Function<T,R> func, F<T> t) // fmap :: (a -> b) -> f a -> f b 
}

@TypeChecked
interface Applicative<A> extends Functor<A> {
    public <T> A<T> pure(T t)
    public <T,R> A<R> ap(A<Function<T,R>> func, A<T> a) // <*> :: f (a -> b) -> f a -> f b 
}

@TypeChecked
interface Monad<M> extends Applicative<M> {
    public <T,R> M<T> unit(T t)
    public <T,R> M<R> bind(M<T> a, Function<T,M<R>> func) //  (>>=) :: m a -> (a -> m b) -> m b
}

@TypeChecked
interface Show<T> {
    String show(T t)
}

@TypeChecked
trait Eq<T> { // One of eq or neq, or both should be overriden.
    boolean eq(T t1, T t2) {
        !this.neq(t1, t2)
    }
    boolean neq(T t1, T t2) {
        !this.eq(t1, t2)
    }
}

@TypeChecked
trait Ord<T> extends Eq<T> {
    boolean gt(T t1, T t2) {
        !this.eq(t1,t2) && !this.lt(t1, t2)
    }
    boolean lt(T t1, T t2) {
        !this.eq(t1,t2) && !this.gt(t1, t2)
    }
    boolean ge(T t1, T t2) {
        this.eq(t1,t2) || this.gt(t1, t2)
    }
    boolean le(T t1, T t2) {
        this.eq(t1,t2) || this.lt(t1, t2)
    }
}

//------ instance ----------
@TypeChecked
class ListFunctor implements Functor<List> {
    @Override
    public <T,R> List<R> fmap(Function<T,R> func, List<T> t) {
        t.collect { func.apply(it) }
    }
}

// instance
@TypeChecked
class OptionalFunctor implements Functor<Optional> {
    @Override
    public <T,R> Optional<R> fmap(Function<T,R> func, Optional<T> t) {
        if (t.isPresent()) {
            return Optional.ofNullable(func.apply(t.get()))
        }
        else {
            return Optional.empty()
        }
    }
}

// type instance
@TypeChecked
class ListApplicative extends ListFunctor implements Applicative<List> {
    @Override
    public <T> List<T> pure(T t) { // pure :: a -> f a
        [t]
    }
    @Override
    public <T,R> List<R> ap(List<Function<T,R>> funcs, List<T> t) { // <*> :: f (a -> b) -> f a -> f b
        (funcs as List<Function<T,R>>).collectMany { func ->
            t.collect{ func.apply(it) } }
    }
}

@TypeChecked
class ListMonad extends ListApplicative implements Monad<List> {
    @Override
    public <T> List<T> unit(T t) {
        super.pure(t)
    }
    @Override
    public <T,R> List<R> bind(List<T> m, Function<T,List<R>> func) { //  (>>=) :: m a -> (a -> m b) -> m b
        m.collectMany { func.apply(it) }
    }
}

@TypeChecked
class IntShow implements Show<Integer> {
    @Override
    public String show(Integer i) {
        i.toString();
    }
}

@TypeChecked
class StringShow implements Show<String> {
    @Override
    public String show(String s) {
        s;
    }
}

@TypeChecked
class ListShow<T> implements Show<List<T>> {
    Show elemShowDict
    ListShow(Show<T> showDict) {
        this.elemShowDict = showDict
    }
    @Override
    public String show(List<T> list) {
        '['+list.collect{ T it -> elemShowDict.show(it) }.join(',')+']'
    }
}

@TypeChecked
class IntEq implements Eq<Integer> {
    @Override
    boolean eq(Integer t1, Integer t2) {
        t1 == t2
    }
}

@TypeChecked
class IntOrd implements Ord<Integer> {
    @Override
    boolean eq(Integer t1, Integer t2) {
        t1 == t2
    }
    @Override
    boolean gt(Integer t1, Integer t2) {
        t1 > t2
    }
}

//--- Static Functions ---
@TypeChecked
class Functions {
    static <T> T mappend(T t1, T t2, Monoid<T> dict=Parameter.IMPLICIT) { dict.mappend(t1,t2) }
    static <T> T mempty(Monoid<T> dict=Parameter.IMPLICIT) { dict.mempty() }
    static <F,T,R> F<R> fmap(Function<T,R> func, F<T> t, Functor<F> dict=Parameter.IMPLICIT) { dict.fmap(func, t)}
    static <A,T,R> A<T> pure(T t, Applicative<A> dict=Parameter.IMPLICIT) { dict.pure(t) }
    static <A,T,R> A<R> ap(A<Function<T,R>> func, A<T> a, Applicative<A> dict=Parameter.IMPLICIT) { dict.ap(func, a) }
    static <M,T,R> M<M> unit(T t, Monad<M> dict=Parameter.IMPLICIT) { dict.unit(t) }
    static <M,T,R> M<R> bind(M<T> a, Function<T,M<R>> func, Monad<M> dict=Parameter.IMPLICIT) { dict.bind(a, func) }
    static <T> String show(T x, Show<T> dict=Parameter.IMPLICIT) { dict.show(x) }

    static <T> boolean eq(T t1, T t2, Eq<T> dict=Parameter.IMPLICIT) { dict.eq(t1, t2) }
    static <T> boolean neq(T t1, T t2, Eq<T> dict=Parameter.IMPLICIT) { dict.neq(t1, t2) }
    static <T> boolean gt(T t1, T t2, Ord<T> dict=Parameter.IMPLICIT) { dict.gt(t1, t2) }
    static <T> boolean lt(T t1, T t2, Ord<T> dict=Parameter.IMPLICIT) { dict.lt(t1, t2) }
    static <T> boolean ge(T t1, T t2, Ord<T> dict=Parameter.IMPLICIT) { dict.ge(t1, t2) }
    static <T> boolean le(T t1, T t2, Ord<T> dict=Parameter.IMPLICIT) { dict.le(t1, t2) }
}

@TypeChecked(extensions='ImplicitParamTransformer.groovy')
class TypeClassTest extends GroovyTestCase {
    void testStringMonoid() {
        def instance_Monoid_java$lang$String_ = new Monoid<String>() {
            @Override String mappend(String i1, String i2) {
                i1+i2
            }
            @Override String mempty() {
                ""
            }
        }
        String s = mempty()
        assert s == ""
        assert mappend("a","b") == "ab"
    }
    //// can't pass due to GROOVY-7170
    // void testSum() {
    //     def instance_Monoid_java$lang$Integer_ = new Monoid<Integer>() {
    //         @Override Integer mappend(Integer i1, Integer i2) {
    //             i1+i2
    //         }
    //         @Override Integer mempty() {
    //             new Integer(0)
    //         }
    //     }
    //     Integer n = mempty()
        
    //     assert mempty(instance_Monoid_java$lang$Integer_) == 0
    //     assert mappend(1, 1) == 2
    //     assert mappend(1, mempty(instance_Monoid_java$lang$Integer_)) == 1
    //     assert mappend(3, 4) == 7
    //     assert mappend(1, mempty(instance_Monoid_java$lang$Integer_)) == 1
    //     assert mappend(3, mempty(instance_Monoid_java$lang$Integer_)) == 3
    // }
    //// can't pass due to GROOVY-7170
    // void testProd() {
    //     def instance_Monoid_java$lang$Integer_ = new Monoid<Integer>() {
    //         @Override Integer mappend(Integer i1, Integer i2) {
    //             i1*i2
    //         }
    //         @Override Integer mempty() {
    //             new Integer(1)
    //         }
    //     }
    //     assert mappend(1, mempty(instance_Monoid_java$lang$Integer_)) == 1
    //     assert mappend(3, 4) == 12
    //     assert mappend(1, mempty(instance_Monoid_java$lang$Integer_)) == 1
    //     assert mappend(3, mempty(instance_Monoid_java$lang$Integer_)) == 3
    // }

    void testListOptionalMonoid() {
        def instance_Monoid_java$util$List_ = new Monoid<List>() {
            @Override List mappend(List i1, List i2) {
                i1+i2 as List
            }
            @Override List mempty() {
                []
            }
        }
        List list = mempty()
        assert mappend([1], list) == [1]

        def instance_Monoid_java$util$Optional_ = new OptionalMonoid<List>(instance_Monoid_java$util$List_)

        Optional emp = mempty()
        assert mappend(Optional.of([1]), emp) == Optional.of([1])
        assert mappend(emp, Optional.of([1])) == Optional.of([1])
    }

    void testListFunctor() {
        def instance_Functor_java$util$List_ = new ListFunctor()
        assert fmap({Integer n-> n*2}, [1,2,3]) == [2,4,6]
        assert fmap({Double n -> (Integer)n}, [1.5d,2.2d,3.3d]) == [1,2,3]
    }

    void testOptionalFunctor() {
        def instance_Functor_java$util$Optional_ = new OptionalFunctor()
        assert fmap({Integer n -> n*2}, Optional.of(3)) == Optional.of(6)
        assert fmap({Integer n -> n*2}, Optional.empty()) == Optional.empty()
    }

    void testListApplicative() {
        def instance_Applicative_java$util$List_ = new ListApplicative()
        assert pure(3, instance_Applicative_java$util$List_) == [3]
        assert ap([{Integer n->n+2} as Function, {Integer n->n+3} as Function] as ArrayList, // due to GROOVY-7147
                  [10,20,30]) == [12,22,32,13,23,33]
    }

    void testListMonad() {
        def instance_Monad_java$util$List_  = new ListMonad()
        assert unit(10, instance_Monad_java$util$List_) == [10]
        assert bind([10,20,30], { Integer it -> [it+1, it] }) == [11, 10, 21, 20, 31, 30]
        assert bind((1..5).collect{(Integer)it}, { Integer a ->
            bind((1..a).collect{(Integer)it}, { Integer b ->
                bind((1..a+b).collect{(Integer)it}, { Integer c ->
                    unit("(a=$a,b=$b,c=$c)", instance_Monad_java$util$List_ )
                     })
                 })
                    }, instance_Monad_java$util$List_).size() == 90
    }

    void testIntShow() {
        def instance_Show_java$lang$Integer_ = new IntShow()
        assert show(3) == "3"
    }

    void testStringListShow() {
        def instance_Show_java$lang$String_ = new StringShow()
        assert show("abc") == "abc"

        def instance_Show_java$util$List_ = new ListShow(instance_Show_java$lang$String_)
        List<String> list = ["a","b","c"]
        assert show(list) == "[a,b,c]"
    }

    void testIntEq() {
        def instance_Eq_java$lang$Integer_ = new IntEq()
        assert eq(1, 2) == false
        assert eq(1, 1) == true
        assert neq(1, 1) == false
        assert neq(1, 2) == true
    }

    void testIntOrd() {
        def instance_Ord_java$lang$Integer_ = new IntOrd()
        assert gt(2, 1) == true
        assert gt(2, 2) == false
        assert gt(2, 3) == false
        assert lt(2, 1) == false
        assert lt(2, 2) == false
        assert lt(2, 3) == true
        assert ge(2, 1) == true
        assert ge(2, 2) == true
        assert ge(2, 3) == false
        assert le(2, 1) == false
        assert le(2, 2) == true
        assert le(2, 3) == true
    }
}

junit.textui.TestRunner.run(TypeClassTest)
