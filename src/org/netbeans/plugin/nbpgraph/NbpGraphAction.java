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

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.netbeans.api.project.Project;
import org.netbeans.modules.apisupport.project.NbModuleProject;
import org.netbeans.plugin.nbpgraph.DependencyExporter.Dependency;
import org.netbeans.spi.project.SubprojectProvider;
import org.openide.awt.DynamicMenuContent;
import org.openide.awt.Mnemonics;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Mathieu Bastian
 */
public class NbpGraphAction extends AbstractAction implements ContextAwareAction {

    public void actionPerformed(ActionEvent e) {
        assert false;
    }

    public Action createContextAwareInstance(Lookup context) {
        return new ContextAction(context);
    }

    private boolean enable(Project p) {
        assert p != null;
        SubprojectProvider sub = p.getLookup().lookup(SubprojectProvider.class);
        if (sub != null) {
            Set<? extends Project> set = sub.getSubprojects();
            for (Project subProject : set) {
                if (!(subProject instanceof NbModuleProject)) {
                    return false;
                }
            }
            if (set.size() > 0) {
                return true;
            }
        }
        return false;
    }

    private String labelFor(Project p) {
        assert p != null;
        return NbBundle.getMessage(NbpGraphAction.class, "DependencyExporter.menu");
    }

    private void perform(Project p, boolean withLibraries) {
        assert p != null;
        SubprojectProvider sub = p.getLookup().lookup(SubprojectProvider.class);
        if (sub != null) {
            List<Dependency> deps = DependencyExporter.export(sub.getSubprojects(), withLibraries);
            DependencyExporter.exportDOT(deps);
        }
    }

    private final class ContextAction extends AbstractAction implements Presenter.Popup {

        private final Project p;

        public ContextAction(Lookup context) {
            Project _p = context.lookup(Project.class);
            p = (_p != null && enable(_p)) ? _p : null;
        }

        public void actionPerformed(ActionEvent e) {
            int res = JOptionPane.showConfirmDialog(null, NbBundle.getMessage(NbpGraphAction.class, "DependencyExporter.library.message"),
                    NbBundle.getMessage(NbpGraphAction.class, "DependencyExporter.library.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            perform(p, res == JOptionPane.YES_OPTION);
        }

        public JMenuItem getPopupPresenter() {
            class Presenter extends JMenuItem implements DynamicMenuContent {

                public Presenter() {
                    super(ContextAction.this);
                }

                public JComponent[] getMenuPresenters() {
                    if (p != null) {
                        Mnemonics.setLocalizedText(this, labelFor(p));
                        return new JComponent[]{this};
                    } else {
                        return new JComponent[0];
                    }
                }

                public JComponent[] synchMenuPresenters(JComponent[] items) {
                    return getMenuPresenters();
                }
            }
            return new Presenter();
        }
    }
}
