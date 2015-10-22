package org.exoplatform.wiki.addons.rdbms.listener;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wiki.ext.impl.WikiSpaceActivityPublisher;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.WikiService;

public class WikiActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(WikiActivityUpdaterListener.class);
  private final WikiService service;

  /**
   * Do not remove wiki service on constructor, puts the service to 
   * the constructor to make sure the service already created before using.
   * 
   * @param wikiService 
   * 
   */
  public WikiActivityUpdaterListener(WikiService wikiService) {
    this.service = wikiService;
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (WikiSpaceActivityPublisher.WIKI_APP_ID.equals(activity.getType())) {
      String newActivityId = event.getData();
      if (!activity.isComment()) {
        LOG.info(String.format("Migration the wiki activity '%s' with new id:: %s", activity.getTitle(), newActivityId));
        String pageId = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_ID_KEY);
        if (pageId == null) return;
        String pageType = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_TYPE_KEY);
        String pageOwner = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_OWNER_KEY);
        //
        Page page = service.getPageByRootPermission(pageType, pageOwner, pageId);
        if (page != null) {
          Node node = page.getJCRPageNode();
          if (node != null && node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
            try {
              node.setProperty(ActivityTypeUtils.EXO_ACTIVITY_ID, newActivityId);
              node.save();
            } catch (RepositoryException e) {
              LOG.warn("Updates the wiki activity is unsuccessful!");
              LOG.debug("Updates the wiki activity is unsuccessful!", e);
            }
          } else {
            LOG.warn("Updates the wiki activity is unsuccessful. Because, the can not get node from wiki page " + pageId);
          }
        } else {
          LOG.warn("Updates the wiki activity is unsuccessful. Because, the can not get the wiki page " + pageId);
        }
      }
    }
  }
}