package org.jenkinsci.plugins.p4.reverting;

import java.io.IOException;
import java.util.logging.Logger;

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
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import jenkins.model.Jenkins;

import org.jenkinsci.plugins.p4.PerforceScm;
import org.jenkinsci.plugins.p4.tasks.RevertTask;
import org.jenkinsci.plugins.p4.workspace.Expand;
import org.jenkinsci.plugins.p4.workspace.Workspace;
import org.kohsuke.stapler.DataBoundConstructor;

import com.perforce.p4java.option.client.RevertFilesOptions;

public class Revert extends Notifier {

	protected static final Logger LOGGER = Logger.getLogger(Revert.class
			.getName());

	@DataBoundConstructor
	public Revert() {
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) throws InterruptedException {

		AbstractProject<?, ?> project = build.getParent();
		SCM scm = project.getScm();

		if (scm instanceof PerforceScm) {
			PerforceScm p4 = (PerforceScm) scm;
			String credential = p4.getCredential();
			Workspace workspace = p4.getWorkspace();
			FilePath buildWorkspace = build.getWorkspace();
			try {
				return revert(build, credential, workspace, buildWorkspace, listener);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	protected boolean revert(Run<?, ?> run, String credential, Workspace workspace, FilePath buildWorkspace,
			TaskListener listener) throws IOException, InterruptedException {

		// Setup Revert Task
		RevertTask task = new RevertTask();
		task.setListener(listener);
		task.setCredential(credential);

		// Set workspace used for the Task
		Workspace ws = task.setEnvironment(run, workspace, buildWorkspace);
		task.setWorkspace(ws);

		return buildWorkspace.act(task);
	}

	public static DescriptorImpl descriptor() {
		return Jenkins.getInstance().getDescriptorByType(
				Revert.DescriptorImpl.class);
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> {

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Perforce: Post Build Revert";
		}

	}
}
