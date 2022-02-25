package hudson.plugins.backlog;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.branch.MultiBranchProject;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.model.TransientActionFactory;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * Property for {@link AbstractProject} that stores the associated Backlog
 * website URL.
 * 
 * @see <a href="http://d.hatena.ne.jp/cactusman/20090328/p1">http://d.hatena.ne.jp/cactusman/20090328/p1</a>
 * @author dragon3
 */
public final class BacklogProjectProperty extends
		JobProperty<Job<?, ?>> {

	public final String url;
	public final String userId;
	private final Secret password;
	private final Secret apiKey;

	@DataBoundConstructor
	public BacklogProjectProperty(final String url, final String userId,
								  final String password, final String apiKey) {

		// normalize
		if (StringUtils.isNotEmpty(url)) {
			if (url.contains("/projects/")) {
				this.url = url;
			} else if (url.endsWith("/")) {
				this.url = url;
			} else {
				this.url = url + '/';
			}
		} else {
			this.url = null;
		}

		this.userId = userId;
		this.password = Secret.fromString(password);
		this.apiKey = Secret.fromString(apiKey);
	}

	public Secret getPassword() {
		return password;
	}

	public Secret getApiKey() {
		return apiKey;
	}

	public String getSpaceURL() {
		if (url == null) {
			return null;
		}

		if (url.contains("/projects/")) {
			return url.substring(0, url.indexOf("/projects/") + 1);
		} else {
			return url;
		}

	}

	public String getProject() {
		if (url == null) {
			return null;
		}
		if (!url.contains("/projects/")) {
			return null;
		}

		return url.substring(url.indexOf("/projects/") + "/projects/".length());
	}

	@Override
	public Action getJobAction(Job<?, ?> job) {
		return new BacklogLinkAction(this);
	}

	@Extension
	public static final class DescriptorImpl extends JobPropertyDescriptor {

		public DescriptorImpl() {
			super(BacklogProjectProperty.class);
			load();
		}

		@Override
		public boolean isApplicable(Class<? extends Job> jobType) {
			return ParameterizedJobMixIn.ParameterizedJob.class.isAssignableFrom(jobType);
		}

		@Override
		public String getDisplayName() {
			return Messages.BacklogProjectProperty_DisplayName();
		}

		public FormValidation doCheckUrl(@QueryParameter String url) {
			try {
				new URL(url);
			} catch (MalformedURLException e) {
				return FormValidation.error(Messages
						.BacklogSecurityRealm_Url_Error());
			}

			return FormValidation.ok();
		}

		public FormValidation doCheckUserId(@QueryParameter String userId) {
			if (StringUtils.isEmpty(userId) || userId.matches("[A-Za-z0-9-_@.]+")) {
				return FormValidation.ok();
			} else {
				return FormValidation.error(Messages
						.BacklogProjectProperty_UserId_Error());
			}
		}

		@Override
		public JobProperty<?> newInstance(StaplerRequest req,
				JSONObject formData) throws FormException {

			if (formData.isEmpty()) {
				return null;
			}

			BacklogProjectProperty bpp = req.bindJSON(
					BacklogProjectProperty.class,
					formData.getJSONObject("backlog"));
			return bpp;
		}
	}

	@Extension
	public static class Factory extends TransientActionFactory<WorkflowJob> {

		@Override
		public Class<WorkflowJob> type() {
			return WorkflowJob.class;
		}

		@NonNull
		@Override
		public Collection<? extends Action> createFor(@NonNull WorkflowJob target) {
			if (target.getParent() instanceof MultiBranchProject) {
				// Instead, add BacklogPullRequestLinkAction in BacklogPullRequestBranchProperty
				return Collections.emptySet();
			}

			return Collections.singleton(new BacklogLinkAction(target.getProperty(BacklogProjectProperty.class)));
		}
	}

}
