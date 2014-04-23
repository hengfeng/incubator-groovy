/*
 * Copyright 2008-2014 the original author or authors.
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
package org.codehaus.groovy.transform

import gls.CompilableTestSupport

/**
 * @author Marcin Grzejszczak
 * @author Paul King
 */
class BuilderTransformTest extends CompilableTestSupport {

    void testSimpleBuilder() {
        assertScript """
            import groovy.transform.builder.*

            @Builder(builderStrategy=SimpleStrategy)
            class Person {
                String firstName
                String lastName
                int age
            }
            def person = new Person().setFirstName("Robert").setLastName("Lewandowski").setAge(21)
            assert person.firstName == "Robert"
            assert person.lastName == "Lewandowski"
            assert person.age == 21
         """
    }

    void testSimpleBuilderInvalidUseOfForClass() {
        def message = shouldNotCompile """
            import groovy.transform.builder.*
            @Builder(builderStrategy=SimpleStrategy, forClass=String)
            class Person { }
        """
        assert message.contains("Annotation attribute 'forClass' not supported")
    }

    void testSimpleBuilderInvalidUseOfBuilderClassName() {
        def message = shouldNotCompile """
            import groovy.transform.builder.*
            @Builder(builderStrategy=SimpleStrategy, builderClassName='Creator')
            class Person { }
        """
        assert message.contains("Annotation attribute 'builderClassName' not supported")
    }

    void testSimpleBuilderInvalidUseOfBuilderMethodName() {
        def message = shouldNotCompile """
            import groovy.transform.builder.*
            @Builder(builderStrategy=SimpleStrategy, builderMethodName='creator')
            class Person { }
        """
        assert message.contains("Annotation attribute 'builderMethodName' not supported")
    }

    void testSimpleBuilderInvalidUseOfBuildMethodName() {
        def message = shouldNotCompile """
            import groovy.transform.builder.*
            @Builder(builderStrategy=SimpleStrategy, buildMethodName='create')
            class Person { }
        """
        assert message.contains("Annotation attribute 'buildMethodName' not supported")
    }

    void testSimpleBuilderCustomPrefix() {
        assertScript """
            import groovy.transform.builder.*

            @Builder(builderStrategy=SimpleStrategy, prefix="")
            class Person {
                String firstName
                String lastName
                int age
            }
            def person = new Person()
            person.firstName("Robert").lastName("Lewandowski")
            person.setAge(21) // normal setters remain but can't be chained
            assert person.firstName == "Robert"
            assert person.lastName == "Lewandowski"
            assert person.age == 21
        """
    }

    void testDefaultBuilder() {
        def shell = new GroovyShell()
        shell.parse """
            import groovy.transform.builder.Builder

            @Builder
            class Person {
                String firstName
                String lastName
                int age
            }
        """
        shell.evaluate """
            def builder = new Person.PersonBuilder()
            def person = builder.firstName("Robert").lastName("Lewandowski").age(21).build()
            assert person.firstName == "Robert"
            assert person.lastName == "Lewandowski"
            assert person.age == 21
        """
    }

    void testDefaultBuilderUsingBuilderMethod() {
        assertScript """
            import groovy.transform.builder.Builder

            @Builder
            class Person {
                String firstName
                String lastName
                int age
            }

            def builder = Person.builder()
            def person = builder.firstName("Robert").lastName("Lewandowski").age(21).build()
            assert person.firstName == "Robert"
            assert person.lastName == "Lewandowski"
            assert person.age == 21
        """
    }

    void testDefaultBuilderCustomNames() {
        def shell = new GroovyShell()
        shell.parse """
            import groovy.transform.builder.Builder

            @Builder(builderClassName="Foo", buildMethodName="create")
            class Person {
                String firstName
                String lastName
                int age
            }
        """
        shell.evaluate """
            def builder = new Person.Foo()
            def person = builder.firstName("Robert").lastName("Lewandowski").age(21).create()
            assert person.firstName == "Robert"
            assert person.lastName == "Lewandowski"
            assert person.age == 21
        """
    }

    void testExternalBuilder() {
        assertScript """
            import groovy.transform.builder.*

            class Person {
                String firstName
                String lastName
            }

            @Builder(builderStrategy=ExternalStrategy, forClass = Person)
            class PersonBuilder { }

            def person = new PersonBuilder().firstName("Robert").lastName("Lewandowski").build()
            assert person.firstName == "Robert"
            assert person.lastName == "Lewandowski"
        """
    }

    void testExternalBuilderInvalidUseOfBuilderClassName() {
        def message = shouldNotCompile """
            import groovy.transform.builder.*
            @Builder(builderStrategy=ExternalStrategy, forClass=String, builderClassName='Creator')
            class DummyStringBuilder { }
        """
        assert message.contains("Annotation attribute 'builderClassName' not supported")
    }

    void testExternalBuilderInvalidUseOfBuilderMethodName() {
        def message = shouldNotCompile """
            import groovy.transform.builder.*
            @Builder(builderStrategy=ExternalStrategy, forClass=String, builderMethodName='creator')
            class DummyStringBuilder { }
        """
        assert message.contains("Annotation attribute 'builderMethodName' not supported")
    }

    void testExternalBuilderCustomPrefix() {
        assertScript """
            import groovy.transform.builder.*

            class Person {
                String firstName
                String lastName
            }

            @Builder(builderStrategy=ExternalStrategy, forClass = Person, prefix = 'set')
            class PersonBuilder1 { }
            @Builder(builderStrategy=ExternalStrategy, forClass = Person, prefix = 'with')
            class PersonBuilder2 { }

            def p1 = new PersonBuilder1().setFirstName("Robert").setLastName("Lewandowski").build()
            p1.with { assert firstName == "Robert" && lastName == "Lewandowski" }
            def p2 = new PersonBuilder2().withFirstName("Robert").withLastName("Lewandowski").build()
            p2.with { assert firstName == "Robert" && lastName == "Lewandowski" }
        """
    }

    void testExternalBuilderWithIncludeAndCustomMethodName() {
        assertScript """
            import groovy.transform.builder.*
            import groovy.transform.Canonical

            @Canonical
            class Person {
                String firstName
                String lastName
            }

            @Builder(builderStrategy=ExternalStrategy, forClass = Person, includes = ['firstName'], buildMethodName="create")
            class PersonBuilder { }

            def personBuilder = new PersonBuilder()
            def person = personBuilder.firstName("Robert").create()
            assert person.firstName == "Robert"
            assert personBuilder.metaClass.methods.find { it.name == "lastName" } == null
            assert personBuilder.metaClass.methods.find { it.name == "firstName" } != null
        """
    }

    void testExternalBuilderWithExclude() {
        assertScript """
            import groovy.transform.builder.*

            class Person {
                String firstName
                String lastName
            }

            @Builder(builderStrategy=ExternalStrategy, forClass = Person, excludes = ['lastName'])
            class PersonBuilder { }

            def personBuilder = new PersonBuilder()
            def person = personBuilder.firstName("Robert").build()
            assert person.firstName == "Robert"
            assert personBuilder.metaClass.methods.find { it.name == "lastName" } == null
            assert personBuilder.metaClass.methods.find { it.name == "firstName" } != null
        """
    }

    void testInitializerStrategy() {
        assertScript '''
            import groovy.transform.builder.*
            import groovy.transform.*

            @ToString
            @Builder(builderStrategy=InitializerStrategy)
            class Person {
                String firstName
                String lastName
                int age
            }

            @CompileStatic
            def method() {
                assert new Person(Person.createInitializer().firstName("John").lastName("Smith").age(21)).toString() == 'Person(John, Smith, 21)'
            }
            // static case
            method()
            // dynamic case
            assert new Person(Person.createInitializer().firstName("John").lastName("Smith").age(21)).toString() == 'Person(John, Smith, 21)'
        '''
        def message = shouldNotCompile '''
            import groovy.transform.builder.*
            import groovy.transform.CompileStatic

            @Builder(builderStrategy=InitializerStrategy)
            class Person {
                String firstName
                String lastName
                int age
            }

            @CompileStatic
            def method() {
                new Person(Person.createInitializer().firstName("John").lastName("Smith"))
            }
        '''
        assert message.contains('[Static type checking] - Cannot find matching method Person#<init>')
        assert message =~ /.*SET.*SET.*UNSET.*/
    }

}
