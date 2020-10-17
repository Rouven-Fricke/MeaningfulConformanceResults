package org.processmining.behavioralspaces.plugins;

import java.io.IOException;

import javax.swing.*;

import org.processmining.behavioralspaces.visualization.DotFileBuilder;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginLevel;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.graphviz.dot.Dot;
import org.processmining.plugins.graphviz.visualisation.DotPanel;

//@Plugin(name = "Dot visualisation", returnLabels = { "Dot visualisation" }, returnTypes = { JComponent.class }, parameterLabels = { "Dot" }, userAccessible = true, level = PluginLevel.Regular)
public class PresenterFrame {

	@Plugin(name = "Dot visualisation", returnLabels = { "Dot visualisation" }, returnTypes = { DotFileBuilder.class }, parameterLabels = { "Dot" }, userAccessible = true, level = PluginLevel.Regular)
	@UITopiaVariant(uiLabel = "Dot visualisation", affiliation = "University of Mannheim", author = "Rouven Fricke", email = "rfricke@mail.uni-mannheim.de")
	@PluginVariant(variantLabel = "Testbed for Rouven", requiredParameterLabels = {0})
	@Visualizer
	public DotFileBuilder visualize(PluginContext context, DotFileBuilder dt) throws IOException {
		System.out.println("=======Started visualization Plugin================");
		return dt.showVisualization();
	}
}
