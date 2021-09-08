package com.abstratt.simon.compiler.source;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.abstratt.simon.metamodel.Metamodel.Type;

import one.util.streamex.StreamEx;

public class MetamodelSourceChain<T extends Type> implements MetamodelSource<T> {
	private final List<MetamodelSource<T>> sources;
	
	public MetamodelSourceChain(List<MetamodelSource<T>> sources) {
		super();
		this.sources = sources;
	}

	@Override
	public T resolveType(String typeName, Set<String> languages) {
		return sources.stream()//
				.map(s -> s.resolveType(typeName, languages))//
				.filter(Objects::nonNull)//
				.findFirst()//
				.orElse(null);
	}

	@Override
	public Stream<T> enumerate(Set<String> languages) {
		return sources.stream()//
				.map(s -> s.enumerate(languages))//
				.map(StreamEx::of)
				.reduce(StreamEx.empty(), StreamEx::append);
	}
	
	@Override
	public SourceProvider builtInSources() {
		return sources.stream()//
				.map(s -> s.builtInSources())//
				.collect(Collectors.collectingAndThen(Collectors.toList(), SourceProviderChain::new));
	}
	
	public static class Factory<T extends Type> implements MetamodelSource.Factory<T> {
		public Factory(List<MetamodelSource.Factory<T>> sourceFactories) {
			this.factories = sourceFactories;
		}

		private final List<MetamodelSource.Factory<T>> factories;

		@Override
		public MetamodelSource<T> build() {
			var sources = factories.stream().map(MetamodelSource.Factory::build).collect(Collectors.toList());
			return new MetamodelSourceChain<T>(sources);
		}
	}

}
