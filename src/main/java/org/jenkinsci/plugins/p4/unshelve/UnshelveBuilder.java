package org.jenkinsci.plugins.p4.unshelve;

import java.io.IOException;
import java.util.logging.Logger;

import org.jenkinsci.plugins.p4.PerforceScm;
import org.jenkinsci.plugins.p4.tasks.UnshelveTask;
import org.jenkinsci.plugins.p4.workspace.Workspace;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;

public class UnshelveBuilder extends Builder {

	private final String shelf;
	private final String resolve;
	private final boolean revert;

	private static Logger logger = Logger.getLogger(UnshelveBuilder.class.getName());

	@DataBoundConstructor
	public UnshelveBuilder(String shelf, String resolve, boolean revert) {
		this.shelf = shelf;
		this.resolve = resolve;
		this.revert = revert;
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public String getShelf() {
		return shelf;
	}

	public String getResolve() {
		return resolve;
	}

	public boolean isRevert() {
		return revert;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {

		AbstractProject<?, ?> project = build.getParent();
		SCM scm = project.getScm();

		if (scm instanceof PerforceScm) {
			PerforceScm p4 = (PerforceScm) scm;
			String credential = p4.getCredential();
			Workspace workspace = p4.getWorkspace();
			FilePath buildWorkspace = build.getWorkspace();
			try {
				return unshelve(build, credential, workspace, buildWorkspace, listener);
			} catch (IOException e) {
				logger.warning("Unable to Unshelve");
				e.printStackTrace();
			} catch (InterruptedException e) {
				logger.warning("Unable to Unshelve");
				e.printStackTrace();
			}
		}

		return false;
	}

	protected boolean unshelve(Run<?, ?> run, String credential, Workspace workspace, FilePath buildWorkspace,
			TaskListener listener) throws IOException, InterruptedException {

		// Setup Unshelve Task
		UnshelveTask task = new UnshelveTask(resolve, revert);
		task.setListener(listener);
		task.setCredential(credential);

		// Set workspace used for the Task
		Workspace ws = task.setEnvironment(run, workspace, buildWorkspace);

		// Expand shelf ${VAR} as needed and set as LABEL
		String id = ws.getExpand().format(shelf, false);
		int change = Integer.parseInt(id);
		task.setShelf(change);
		task.setWorkspace(ws);

		return buildWorkspace.act(task);
	}

	public static DescriptorImpl descriptor() {
		return Jenkins.getInstance().getDescriptorByType(UnshelveBuilder.DescriptorImpl.class);
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

		@Override
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Perforce: Unshelve";
		}
	}
}
