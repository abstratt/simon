package com.abstratt.simon.compiler.ecore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.abstratt.simon.examples.ui.UI;
import org.junit.jupiter.api.Test;

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
