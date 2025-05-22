package com.ascelion.guice.internal;

import static com.ascelion.guice.internal.GuiceUtils.isVetoed;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import com.ascelion.guice.GuiceScan;
import com.ascelion.guice.internal.BootstrapContext.InjectionInfo.InjectionInfoBuilder;
import com.google.inject.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Stream;

import io.github.classgraph.*;
import jakarta.inject.Inject;
import lombok.*;

public final class BootstrapContext {

	@Builder
	@RequiredArgsConstructor
	@Getter
	public static class InjectionInfo {
		private final Class<?> type;
		@Singular
		private final List<Executable> executables;
		@Singular
		private final List<Field> fields;
	}

	private final ScanResult scanned;
	private final Set<String> beans = new TreeSet<>();
	private final Map<Class<? extends Annotation>, Scope> scopes = new IdentityHashMap<>();

	@Inject
	public BootstrapContext(@GuiceScan ScanResult scanned) {
		this.scanned = scanned;
		this.scopes.put(com.google.inject.Singleton.class, Scopes.SINGLETON);
	}

	public void addBean(ClassInfo ci) {
		this.beans.add(ci.getName());
	}

	public void addBean(Class cl) {
		this.beans.add(cl.getName());
	}

	public void addScope(Scope scope, Class<? extends Annotation> annotation) {
		this.scopes.put(annotation, scope);
	}

	public Scope getScope(AnnotatedElement... annotated) {
		return Stream.of(annotated)
				.map(GuiceUtils::scopeAnnotation)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.map(this::requireScope)
				.findFirst()
				.orElse(Scopes.NO_SCOPE);
	}

	private Scope requireScope(Class<? extends Annotation> annotation) {
		return this.scopes.computeIfAbsent(annotation, a -> {
			throw new ProvisionException("Cannot find scope for @" + a.getName());
		});
	}

	public ClassInfoList getAllStandardClasses() {
		return this.scanned.getAllStandardClasses()
				.filter(this::isEligible);
	}

	public ClassInfoList getClassesImplementing(Class<?> type) {
		return this.scanned.getClassesImplementing(type)
				.filter(this::isEligible);
	}

	public ClassInfoList getClassesWithFieldAnnotation(Class<? extends Annotation> type) {
		return this.scanned.getClassesWithFieldAnnotation(type)
				.filter(this::isEligible);
	}

	public ClassInfoList getClassesWithMethodAnnotation(Class<? extends Annotation> type) {
		return this.scanned.getClassesWithMethodAnnotation(type)
				.filter(this::isEligible);
	}

	public List<InjectionInfo> getClassesWithInjectedType(Class<?> type) {
		final String typeName = type.getName();
		final List<InjectionInfo> injections = new ArrayList<>();

		for (final ClassInfo ci : getAllStandardClasses()) {
			final var iiBld = InjectionInfo.builder().type(ci.loadClass());

			visitExecutables(ci, typeName, iiBld);
			visitFields(ci, typeName, iiBld);

			if (isEmpty(iiBld.executables) && isEmpty(iiBld.fields)) {
				continue;
			}

			injections.add(iiBld.build());
		}

		return unmodifiableList(injections);
	}

	private void visitExecutables(ClassInfo ci, String type, InjectionInfoBuilder bld) {
		for (final var mi : ci.getMethodAndConstructorInfo()) {
			if (mi.isAbstract() || mi.isBridge() || mi.isNative() || mi.isSynthetic()) {
				continue;
			}

			final var hasType = Stream.of(mi.getParameterInfo())
					.map(MethodParameterInfo::getTypeDescriptor)
					.filter(Objects::nonNull)
					.map(TypeSignature::toString)
					.anyMatch(type::equals);

			if (!hasType) {
				continue;
			}

			if (mi.hasAnnotation(com.google.inject.Inject.class) || mi.hasAnnotation(jakarta.inject.Inject.class)) {
				if (hasType && mi.isConstructor()) {
					bld.executable(mi.loadClassAndGetConstructor());
				} else {
					bld.executable(mi.loadClassAndGetMethod());
				}
			}
		}
	}

	private void visitFields(ClassInfo ci, String type, InjectionInfoBuilder bld) {
		for (final var fi : ci.getFieldInfo()) {
			if (fi.isFinal() || fi.isSynthetic() || fi.isTransient()) {
				continue;
			}
			if (fi.getTypeDescriptor() == null) {
				continue;
			}
			if (!type.equals(fi.getTypeDescriptor().toString())) {
				continue;
			}
			if (fi.hasAnnotation(com.google.inject.Inject.class) || fi.hasAnnotation(jakarta.inject.Inject.class)) {
				bld.field(fi.loadClassAndGetField());
			}
		}
	}

	private boolean isEligible(ClassInfo ci) {
		if (this.beans.contains(ci.getName())) {
			return false;
		}

		if (isVetoed(ci)) {
			return false;
		}

		if ((ci.getTypeSignature() != null) && !isEmpty(ci.getTypeSignature().getTypeParameters())) {
			return false;
		}

		if (hasSimpleConstructor(ci)) {
			return true;
		}
		if (hasInjectConstructor(ci)) {
			return true;
		}

		return false;
	}

	private boolean hasSimpleConstructor(ClassInfo ci) {
		return ci.getDeclaredConstructorInfo()
				.filter(mi -> mi.getParameterInfo().length == 0)
				.size() == 1;
	}

	private boolean hasInjectConstructor(ClassInfo ci) {
		return ci.getDeclaredConstructorInfo()
				.filter(mi -> mi.hasAnnotation(jakarta.inject.Inject.class)
						|| mi.hasAnnotation(com.google.inject.Inject.class))
				.size() == 1;
	}
}
