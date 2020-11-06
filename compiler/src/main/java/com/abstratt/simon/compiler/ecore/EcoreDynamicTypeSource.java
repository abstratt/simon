package com.abstratt.simon.compiler.ecore;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import com.abstratt.simon.Meta.Package;
import com.abstratt.simon.compiler.TypeSource;
import com.abstratt.simon.compiler.ecore.EcoreMetamodel.EcoreType;
import com.abstratt.simon.java2ecore.Java2EcoreMapper;

public class EcoreDynamicTypeSource implements TypeSource<EcoreType<EClassifier>> {

	Map<String, EPackage> packages;
	private Reflections reflections;
	public EcoreDynamicTypeSource() {
		this.reflections = new Reflections(new ConfigurationBuilder()
			     .setUrls(ClasspathHelper.forClassLoader()));
		
		Set<Class<?>> packageImplementations = reflections.getTypesAnnotatedWith(Package.class);
		Java2EcoreMapper mapper = new Java2EcoreMapper();
		this.packages = packageImplementations.stream().collect(Collectors.toMap(Class::getName, packageClass -> mapper.map(packageClass)));
		System.out.println(packages);
	}
	
	@Override
	public EcoreType<EClassifier> resolveType(String typeName) {
		Collection<EPackage> packages = this.packages.values();
		return packages //
				.stream() //
				.map(ePackage -> ePackage.getEClassifier(typeName)) //
				.filter(it -> it != null) //
				.map(eClass -> (EcoreType<EClassifier>) EcoreType.fromClassifier(eClass)) //
				.findAny().orElse(null);
	}
	
	@Override
	public void use(String sourceName) {
		
	}
}
