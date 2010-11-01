/*Copyright (c) 2010 Mathieu Bastian <mathieu.bastian@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package org.netbeans.plugin.nbpgraph;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.apisupport.project.ModuleDependency;
import org.netbeans.modules.apisupport.project.NbModuleProject;
import org.netbeans.modules.apisupport.project.ProjectXMLManager;
import org.netbeans.modules.apisupport.project.universe.ModuleEntry;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 *
 * @author Mathieu Bastian
 */
public class DependencyExporter {

    private static final String LAST_PATH = "DependencyExporter_Last_Path";

    public static List<Dependency> export(Set<? extends Project> set, boolean withLibraries) {
        List<Dependency> dependencies = new LinkedList<Dependency>();
        Set<String> projectsModules = new HashSet<String>();

        for (Project subProject : set) {

            if (subProject instanceof NbModuleProject) {
                NbModuleProject nbModuleProject = (NbModuleProject) subProject;
                String projectName = ProjectUtils.getInformation(subProject).getDisplayName();
                projectsModules.add(projectName);
                ProjectXMLManager projectXMLManager;
                try {
                    projectXMLManager = ProjectXMLManager.getInstance(nbModuleProject.getProjectDirectoryFile());
                    for (ModuleDependency dep : projectXMLManager.getDirectDependencies(nbModuleProject.getPlatform(false))) {
                        ModuleEntry depModule = dep.getModuleEntry();
                        Dependency dependency = new Dependency(projectName, depModule.getLocalizedName());
                        dependencies.add(dependency);
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }

        if (!withLibraries) {
            for (Iterator<Dependency> itr = dependencies.iterator(); itr.hasNext();) {
                Dependency d = itr.next();
                if (!projectsModules.contains(d.to)) {
                    itr.remove();
                }
            }
        }

        return dependencies;
    }

    public static void exportDOT(List<Dependency> dependencies) {
        StringBuilder stringBuilder = new StringBuilder();
        final String newLine = System.getProperty("line.separator");

        //Header
        stringBuilder.append("digraph G {");
        stringBuilder.append(newLine);
        stringBuilder.append("ratio=auto;");
        stringBuilder.append(newLine);
        stringBuilder.append("margin=0;");
        stringBuilder.append(newLine);
        stringBuilder.append("node[shape=box,tailport=s];");
        stringBuilder.append(newLine);

        //Graph
        for (Dependency d : dependencies) {
            stringBuilder.append("\"");
            stringBuilder.append(d.from);
            stringBuilder.append("\" -> \"");
            stringBuilder.append(d.to);
            stringBuilder.append("\";");
            stringBuilder.append(newLine);
        }

        //Footer
        stringBuilder.append("}");

        exportDotFile(stringBuilder);
    }

    private static void exportDotFile(StringBuilder stringBuilder) {
        String lastPath = NbPreferences.forModule(DependencyExporter.class).get(LAST_PATH, "");
        final JFileChooser chooser = new JFileChooser(lastPath);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setDialogTitle(NbBundle.getMessage(DependencyExporter.class, "DependencyExporter.filechooser.title"));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("DOT files", "dot");
        chooser.addChoosableFileFilter(filter);
        int returnFile = chooser.showSaveDialog(null);
        if (returnFile != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();

        if (!file.getPath().endsWith(".dot")) {
            file = new File(file.getPath() + ".dot");
        }

        NbPreferences.forModule(DependencyExporter.class).put(LAST_PATH, file.getAbsolutePath());

        final BufferedWriter bufferedWriter;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.append(stringBuilder);
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        String msg = NbBundle.getMessage(DependencyExporter.class, "DependencyExporter.finishedMessage.message", file.getName());
        JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), msg, NbBundle.getMessage(DependencyExporter.class, "DependencyExporter.finishedMessage.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    public static class Dependency {

        private String from;
        private String to;

        public Dependency(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
}
