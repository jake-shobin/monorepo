/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2004, 2005, 2006 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/



package org.sakaiproject.tool.assessment.ui.listener.author;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.ResourceBundle;
import javax.faces.context.FacesContext;
import javax.faces.application.FacesMessage;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.tool.assessment.data.ifc.assessment.SectionAttachmentIfc;
import org.sakaiproject.tool.assessment.data.ifc.assessment.SectionDataIfc;
import org.sakaiproject.tool.assessment.data.ifc.assessment.SectionMetaDataIfc;
import org.sakaiproject.tool.assessment.facade.AssessmentFacade;
import org.sakaiproject.tool.assessment.facade.QuestionPoolFacade;
import org.sakaiproject.tool.assessment.facade.AgentFacade;
import org.sakaiproject.tool.assessment.facade.ItemFacade;
import org.sakaiproject.tool.assessment.facade.SectionFacade;
import org.sakaiproject.tool.assessment.services.ItemService;
import org.sakaiproject.tool.assessment.services.QuestionPoolService;
import org.sakaiproject.tool.assessment.services.assessment.AssessmentService;
import org.sakaiproject.tool.assessment.ui.bean.author.AssessmentBean;
import org.sakaiproject.tool.assessment.ui.bean.author.SectionBean;
import org.sakaiproject.tool.assessment.ui.listener.util.ContextUtil;

/**
 * <p>Title: Samigo</p>2
 * <p>Description: Sakai Assessment Manager</p>
 * <p>Copyright: Copyright (c) 2004 Sakai Project</p>
 * <p>Organization: Sakai Project</p>
 * @author Ed Smiley
 * @version $Id$
 */

public class SavePartListener
    implements ActionListener
{
  private static Log log = LogFactory.getLog(SavePartListener.class);
  //private static ContextUtil cu;

  public SavePartListener()
  {
  }

  public void processAction(ActionEvent ae) throws AbortProcessingException
  {
    FacesContext context = FacesContext.getCurrentInstance();
    //Map reqMap = context.getExternalContext().getRequestMap();
    //Map requestParams = context.getExternalContext().getRequestParameterMap();

    AssessmentBean assessmentBean = (AssessmentBean) ContextUtil.lookupBean(
								   "assessmentBean");
    String assessmentId = assessmentBean.getAssessmentId();

    SectionBean sectionBean= (SectionBean) ContextUtil.lookupBean(
                         "sectionBean");
    // create an assessment based on the title entered and the assessment
    // template selected
    // #1 - read from form editpart.jsp
    String title = (sectionBean.getSectionTitle()).trim();
    String description = sectionBean.getSectionDescription();
    String sectionId = sectionBean.getSectionId();
    AssessmentService assessmentService = new AssessmentService();
    SectionFacade section;
    /*
    if (sectionId.equals("")){
      section = addPart(assessmentId);
      sectionId = section.getSectionId().toString();
    }
    else {
      section = assessmentService.getSection(sectionId);
    }
    */
    //Long assessmentId = section.getAssessmentId();

    boolean addItemsFromPool = false;

    sectionBean.setOutcome("editAssessment");
   
    if((sectionBean.getType().equals("2"))&& (sectionBean.getSelectedPool().equals(""))){
          
	String selectedPool_err=ContextUtil.getLocalizedString("org.sakaiproject.tool.assessment.bundle.AuthorMessages","selectedPool_error");
	context.addMessage(null,new FacesMessage(selectedPool_err));
	sectionBean.setOutcome("editPart");
	return ;

    }

    if (!("".equals(sectionBean.getType()))  && ((SectionDataIfc.RANDOM_DRAW_FROM_QUESTIONPOOL.toString()).equals(sectionBean.getType()))) {
      addItemsFromPool = true;

      if (validateItemsDrawn(sectionBean)) {
        // if the author type was random draw type,  and the new type is random draw , then we need to disassociate sectionid with each items. Cannot delete items, 'cuz these items are linked in the pool
    	  section = getOrAddSection(assessmentService, assessmentId, sectionId);
    	  if( (section !=null) && (section.getSectionMetaDataByLabel(SectionDataIfc.AUTHOR_TYPE)!=null) && (section.getSectionMetaDataByLabel(SectionDataIfc.AUTHOR_TYPE).equals(SectionDataIfc.RANDOM_DRAW_FROM_QUESTIONPOOL.toString()))) {

          assessmentService.removeAllItems(sectionId);
        // need to reload
          section = assessmentService.getSection(sectionId);
        }
      }
      else {
        sectionBean.setOutcome("editPart");
        return;
      }
    }
    else {
    	section = getOrAddSection(assessmentService, assessmentId, sectionId);
    }

    log.debug("**** section title ="+section.getTitle());
    log.debug("**** title ="+title);
    // if (title != null & !title.equals(""))  // There is no spec saying we don't allow empty string for title , SAK-4211
    if (title != null)
      section.setTitle(title);
    section.setDescription(description);

    // TODO: Need to save Type, Question Ordering, and Metadata

    if (!("".equals(sectionBean.getKeyword())))
    section.addSectionMetaData(SectionMetaDataIfc.KEYWORDS, sectionBean.getKeyword());

    if (!("".equals(sectionBean.getObjective())))
    section.addSectionMetaData(SectionMetaDataIfc.OBJECTIVES, sectionBean.getObjective());

    if (!("".equals(sectionBean.getRubric())))
    section.addSectionMetaData(SectionMetaDataIfc.RUBRICS, sectionBean.getRubric());

    if (!("".equals(sectionBean.getQuestionOrdering())))
    section.addSectionMetaData(SectionDataIfc.QUESTIONS_ORDERING, sectionBean.getQuestionOrdering());


    if (!("".equals(sectionBean.getType())))  {
    section.addSectionMetaData(SectionDataIfc.AUTHOR_TYPE, sectionBean.getType());
      if ((SectionDataIfc.RANDOM_DRAW_FROM_QUESTIONPOOL.toString()).equals(sectionBean.getType()))  {
        if ((sectionBean.getNumberSelected()!=null) && !("".equals(sectionBean.getNumberSelected())))
        {
          section.addSectionMetaData(SectionDataIfc.NUM_QUESTIONS_DRAWN, sectionBean.getNumberSelected());
        }

        if (!("".equals(sectionBean.getSelectedPool())))
        {
          section.addSectionMetaData(SectionDataIfc.POOLID_FOR_RANDOM_DRAW, sectionBean.getSelectedPool());
          String poolname = "";
          QuestionPoolService qpservice = new QuestionPoolService();
          QuestionPoolFacade poolfacade = qpservice.getPool(new Long(sectionBean.getSelectedPool()), AgentFacade.getAgentString());
          if (poolfacade!=null) {
            poolname = poolfacade.getTitle();
          }

          section.addSectionMetaData(SectionDataIfc.POOLNAME_FOR_RANDOM_DRAW, poolname);
        }
        
        section.addSectionMetaData(SectionDataIfc.RANDOM_DRAW_TYPE, sectionBean.getRandomDrawType());
      }
    }


    // if author-type is random draw from pool, add all items from pool now
    // Note: a pool can only be randomly drawn by one part.  if part A is created to randomly draw from pool 1, and you create part B, and select  the same pool 1, all items from part A will be removed.  (item.sectionId will be set to sectionId of part B.
    // currently if a pool is selected by one random draw part it will no longer show up in the poollist for random draw 

    if (addItemsFromPool)
    {
      QuestionPoolService qpservice = new QuestionPoolService();
      //ItemService itemservice = new ItemService();

    ArrayList itemlist = qpservice.getAllItems(new Long(sectionBean.getSelectedPool()) );
    int i = 0;
    Iterator iter = itemlist.iterator();
    while(iter.hasNext())
    {
      ItemFacade item= (ItemFacade) iter.next();
      item.setSection(section);
      item.setSequence(new Integer(i+1));
      section.addItem(item);
      i= i+1;
    }
    }

    assessmentService.saveOrUpdateSection(section);

    // added by daisyf, 10/10/06
    updateAttachment(section.getSectionAttachmentList(), sectionBean.getAttachmentList(), section.getData());

    // #2 - goto editAssessment.jsp, so reset assessmentBean
    AssessmentFacade assessment = assessmentService.getAssessment(
        assessmentBean.getAssessmentId());
    assessmentBean.setAssessment(assessment);
    assessmentService.updateAssessmentLastModifiedInfo(assessment);
  }

  public SectionFacade addPart(String assessmentId){
    AssessmentService assessmentService = new AssessmentService();
    SectionFacade section = assessmentService.addSection(
			     assessmentId);
    return section;
  }

  private SectionFacade getOrAddSection(AssessmentService assessmentService, String assessmentId, String sectionId) {
	  SectionFacade section;
	  if (sectionId.equals("")){
		  section = assessmentService.addSection(assessmentId);
		  sectionId = section.getSectionId().toString();
	  }
	  else {
		  section = assessmentService.getSection(sectionId);
	  }
	  return section;
  }

  public boolean validateItemsDrawn(SectionBean sectionBean){
     FacesContext context = FacesContext.getCurrentInstance();
     String numberDrawn = sectionBean.getNumberSelected();
     String err;
    
     QuestionPoolService qpservice = new QuestionPoolService();

     ArrayList itemlist = qpservice.getAllItems(new Long(sectionBean.getSelectedPool()) );
     int itemcount = itemlist.size();
     String itemcountString=" "+Integer.toString(itemcount);

     try{
	 int numberDrawnInt = Integer.parseInt(numberDrawn);
	 if(numberDrawnInt <=0 || numberDrawnInt>itemcount){
	     err=ContextUtil.getLocalizedString("org.sakaiproject.tool.assessment.bundle.AuthorMessages","qdrawn_error");
	     context.addMessage(null,new FacesMessage(err+itemcountString ));
	     return false;

	 }
	
     } catch(NumberFormatException e){
	 err=ContextUtil.getLocalizedString("org.sakaiproject.tool.assessment.bundle.AuthorMessages","qdrawn_error");
	 context.addMessage(null,new FacesMessage(err+itemcountString ));
	 return false;
     }
     
     return true;
           
  }


    private void updateAttachment(List oldList, List newList, SectionDataIfc section){
    if (newList == null || newList.size()==0) return;
    List list = new ArrayList();
    HashMap map = getAttachmentIdHash(oldList);
    for (int i=0; i<newList.size(); i++){
      SectionAttachmentIfc a = (SectionAttachmentIfc)newList.get(i);
      if (map.get(a.getAttachmentId())!=null){
        // exist already, remove it from map
        map.remove(a.getAttachmentId());
      }
      else{
        // new attachments
        a.setSection(section);
        list.add(a);
      }
    }      
    // save new ones
    AssessmentService assessmentService = new AssessmentService();
    assessmentService.saveOrUpdateAttachments(list);

    // remove old ones
    Set set = map.keySet();
    Iterator iter = set.iterator();
    while (iter.hasNext()){
      Long attachmentId = (Long)iter.next();
      assessmentService.removeSectionAttachment(attachmentId.toString());
    }
  }

  private HashMap getAttachmentIdHash(List list){
    HashMap map = new HashMap();
    for (int i=0; i<list.size(); i++){
      SectionAttachmentIfc a = (SectionAttachmentIfc)list.get(i);
      map.put(a.getAttachmentId(), a);
    }
    return map;
  }


}
