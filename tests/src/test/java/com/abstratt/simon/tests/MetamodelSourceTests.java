package com.abstratt.simon.tests;

import com.abstratt.simon.compiler.source.ecore.EcoreDynamicMetamodelSource;
import com.abstratt.simon.examples.ui.UI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MetamodelSourceTests {
	@Test
	void dynamic() {
		var typeSourceFactory = new EcoreDynamicMetamodelSource.Factory(UI.class.getPackageName());
		try (var typeSource = typeSourceFactory.build()) {
			var resolved = typeSource.resolveType("application");
			assertNotNull(resolved);
		}
	}
}
