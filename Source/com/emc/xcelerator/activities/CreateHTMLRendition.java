package com.emc.xcelerator.activities;

import java.util.ArrayList;
import java.util.Locale;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.DfSingleDocbaseModule;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfLogger;
import com.documentum.fc.common.IDfId;
import com.documentum.services.cts.df.profile.ICTSProfile;
import com.documentum.services.cts.df.profile.ICTSProfileFilter;
import com.documentum.services.dam.df.transform.ICTSService;
import com.documentum.services.dam.df.transform.IMediaProfile;
import com.documentum.services.dam.df.transform.IProfileFormat;
import com.documentum.services.dam.df.transform.IProfileParameter;
import com.documentum.services.dam.df.transform.IProfileService;
import com.documentum.services.dam.df.transform.ITransformRequest;


public class CreateHTMLRendition extends DfSingleDocbaseModule{

    public Result execute (String packageToTransform, boolean newObject, String newObjectName, String newObjectType, String rootPathName, String appendPath, String folderObjectType) {
    	Result result = new Result();
		IDfSysObject objectToTransform = null;
	    IMediaProfile profile = null;
	    IDfSession session = null;
	    ArrayList<IMediaProfile> mediaProfiles = null;
	    String targetFormat = "html";
	    IDfSysObject newRepoObject = null;
	    IDfId newRepoObjectID = null;
	    
        try{
        	DfLogger.debug(this,"Inside Create New HTML Rendition Object", null, null);
		    session = getSession();
		    
        	IDfId rootPath = session.getFolderByPath(rootPathName).getObjectId();

		    objectToTransform = (IDfSysObject)session.getObject(new DfId(packageToTransform));
		    
    	    if( objectToTransform == null) {
    	    	String errorMessage = "There isn't an object associated to the package in CreateHTMLRendition";
    	    	DfLogger.debug(this, errorMessage, null, null);
    	    	result.setErrorMessage(errorMessage);
    	    	result.setSuccess(false);
    	    	return result;
    	    }
    	    
		    String srcObjectID = objectToTransform.getString("r_object_id");
    		String srcFormat = objectToTransform.getContentType();
    		String tarFormat = "html";

        	DfLogger.debug(this,"Method called with Parameter Values. " , null, null);
        	DfLogger.debug(this,"Create New Object: " + newObject, null, null);
        	DfLogger.debug(this,"New Object Name: " + newObjectName, null, null);
        	DfLogger.debug(this,"New Object Type: " + newObjectType, null, null);
        	DfLogger.debug(this,"Root Path ID: " + rootPath, null, null);  
        	DfLogger.debug(this,"Append Path: " + appendPath, null, null);
        	DfLogger.debug(this,"Folder Object Type: " + folderObjectType, null, null);        	      	        	
			DfLogger.debug(this,"Source Object ID: " + srcObjectID, null, null);
			DfLogger.debug(this,"Source Format Type	: " + srcFormat, null, null);			
			if(newObject){
				if(newObjectName ==null|| newObjectType.trim().equals("")){
					newObjectName = objectToTransform.getString("object_name");
					DfLogger.debug(this,"New Object Name: " + newObjectName, null, null);					
				}
				if(newObjectType==null || newObjectType.trim().equals("")){
					newObjectType = objectToTransform.getTypeName();
					DfLogger.debug(this,"New Object Type: " + newObjectType, null, null);
				}
				if(rootPath==null || rootPath.getId().equalsIgnoreCase("0000000000000000")){
					rootPath = objectToTransform.getFolderId(0);
					DfLogger.debug(this,"Root Path ID: " + rootPath, null, null);
				}
				DfLogger.debug(this,"Creating New Object. Creating folders.",  null, null);	
				IDfFolder theFolder = this.CreateFolderByIdAndPath( rootPath, appendPath,  folderObjectType, session);
		    	newRepoObject = (IDfSysObject)session.newObject(newObjectType);
		    	newRepoObject.setObjectName(newObjectName);
		    	newRepoObject.setContentType(targetFormat);		    	
		    	DfLogger.debug(this,"New Object Created : " + newRepoObject.getObjectId().getId(),  null, null);	
		        this.LinkObject(newRepoObject, theFolder.getObjectId().toString(), false);	
		        DfLogger.debug(this,"New object linked to the folder." + rootPath+"/"+appendPath,  null, null);
		        newRepoObjectID = newRepoObject.getObjectId();
		        result.setNewObjectId(newRepoObjectID.getId());
			}
			DfLogger.debug(this,"Finding profiles to transform document to HTML",  null, null);
			mediaProfiles = this.findProfile(session, srcObjectID, srcFormat, tarFormat);
        	if(mediaProfiles != null){
    			DfLogger.debug(this, mediaProfiles.size() + " Profiles Found.", null, null);
    	        com.documentum.fc.client.IDfSessionManager idfsessionmanager = session.getSessionManager();
    	        IDfClient idfclient = session.getClient();
    			java.util.Iterator<IMediaProfile> itr = mediaProfiles.iterator();  
				for(;itr.hasNext();){
    				profile = itr.next();
    				if( profile != null) {
    					DfLogger.debug(this, "Profile Name : " + profile.getObjectName(), null, null);
    					DfLogger.debug(this, "Sending Request to create Transformation object using "+profile.getObjectName() , null, null);
    			        ITransformRequest itransformrequest = this.createRenditionTransformRequestNew(session, objectToTransform, profile, targetFormat, 
    			        		profile.getParameters(), newRepoObjectID,false);
    			        ICTSService ictsservice = (ICTSService)idfclient.newService((com.documentum.services.dam.df.transform.ICTSService.class).getName(), 
    			        		idfsessionmanager);
    			        ictsservice.submitRequest(session, itransformrequest, false, false, true);
    				}
    			}
    		}else{
    			String errorMsg = "Formats not supported. Profile not found";
    			DfLogger.debug(this, errorMsg, null, null);
    			result.setErrorMessage(errorMsg);
    			result.setSuccess(false);
    			return result;
    		}
        	DfLogger.debug(this, "HTML Rendition created Successfully"  , null, null);
        	result.setSuccess(true);
    	    return result;
        }catch (DfException e){
        	String errorMsg = "DFC Error creating HTML Rendition";
            DfLogger.error(this, errorMsg, null, e);
            result.setErrorMessage(errorMsg);
            result.setSuccess(false);
        }catch(Exception e){
        	String errorMsg = "Error creating HTML Rendition";
            DfLogger.warn(this, errorMsg, null, e);
            result.setErrorMessage(errorMsg);
            result.setSuccess(false);
        }finally{
        	//sessionManager.release(session);        	
        }
        return result;
    }

    private ArrayList<IMediaProfile> findProfile(IDfSession session, String srcObjectID, String srcFormat, String tarFormat){
    	IDfClientX cx = null;
    	IDfClient client = null;
    	IDfSessionManager sessionManager;
    	IMediaProfile mediaProfile[];
    	IProfileFormat profileFormat;
    	IProfileService profileService;
    	ArrayList<IMediaProfile>  mediaProfileToReturn = null;
    	ICTSProfile profile ;
    	IProfileFormat[] profileFormats;
    	ICTSProfileFilter[] proFilters;
    	ICTSProfileFilter proFilter;

    	try{
    		DfLogger.debug(this,"Finding suitable profile for " + srcObjectID, null, null);
    		cx = new DfClientX();
			client = cx.getLocalClient();
    		sessionManager = session.getSessionManager();
    		//DfLogger.debug(this,"Creating IProfileService object.", null, null);
    		profileService = (IProfileService) client.newService(IProfileService.class.getName(), sessionManager);
    		//DfLogger.debug(this,"ProfileService object created. Getting profiles based on object and source and target formats", null, null);
			mediaProfile = profileService.getProfiles(session, srcFormat, srcObjectID, null);
			if(mediaProfile != null){
				//DfLogger.debug(this,"Number of profiles got " + mediaProfile.length, null, null);
				mediaProfileToReturn = new ArrayList<IMediaProfile>();
    			for(int i = 0; i<mediaProfile.length; i++){
    				//DfLogger.debug(this,"Traversing profiles to get the suitable profile", null, null);
    				profile = (ICTSProfile)mediaProfile[i];
    				profileFormats = profile.getFormats();
    				for(int j=0; j<profileFormats.length; j++){
    					//DfLogger.debug(this,"Checking profile formats for the profile " +  profile.getObjectName(), null, null);
    					profileFormat = profileFormats[j];
    					if(profileFormat.getSourceFormat().equals(srcFormat) && profileFormat.getTargetFormat().equals(tarFormat)){
    						//DfLogger.debug(this,"Source and Target formats matched for the profile " + profile.getObjectName(), null, null);
    						//DfLogger.debug(this,"Getting profile filters", null, null);
    						proFilters = profile.getProfileFilters();
    						if(proFilters != null){
        						for( int k = 0; k <proFilters.length ;k++){
        							proFilter =  proFilters[k];
        							String filterName = proFilter.getFilterName();
        							String filterValues[] = proFilter.getFilterValues();
        							for(int x = 0; x<filterValues.length; x++){
        								String filterValue = filterValues[x];
        								if(filterName != null && filterValue != null){
        									if(filterName.equalsIgnoreCase("RenditonType") && filterValue.equalsIgnoreCase("toHTML")){
        										DfLogger.debug(this,"Profile found - " + profile.getObjectName(), null, null);
        										mediaProfileToReturn.add(profile);
        									}
        								}
        							}
        						}
    						}
    					}
    				}
    			}
			}
    	}catch(Exception e){
    		DfLogger.error(this,"Exception in executing Find Profile", null, e);
    	}finally{
    	}
    	return mediaProfileToReturn;
    }

    private IDfFolder CreateFolderByIdAndPath(IDfId idfid, String appendPath, String folderObjectType, IDfSession idfsession) throws Exception {
        IDfFolder idffolder = null;
        IDfPersistentObject idfpersistentobject = null;
        if (idfid.isNull() || !idfid.isObjectId()) {
            DfLogger.debug(this, "The object id: " + idfid.toString() + " is invalid.  Called from IDfId version of CreateFolderByIdAndPath.", null, null);
            return idffolder;

        }
        idfpersistentobject = idfsession.getObject(idfid);
        if (!(idfpersistentobject instanceof IDfFolder)) {
            DfLogger.debug(this, "The object id: " + idfid.toString() + " is not a dm_folder or subtype.  Called from CreateFolderByIdAndPath.", null, null);
            return idffolder;
        }
        if (appendPath == null || appendPath.length() == 0) {
            return (IDfFolder)idfpersistentobject;
        }
        String as[] = appendPath.split("/");
        idffolder = (IDfFolder)idfpersistentobject;
        String rootPath = idffolder.getFolderPath(0);
        for (int i = 0; i < as.length; i++) {
            String s3 = rootPath;
            rootPath = rootPath + "/" + as[i];
            idffolder = idfsession.getFolderByPath(rootPath);
            if (idffolder != null) {
                continue;
            }
            if (i == as.length - 1) {
                if (idfsession.getType(folderObjectType) == null) {
                    DfLogger.debug(this, "Tried to create an object of type: " + folderObjectType + " which does not exist in the repository.  Called from CreateFolderByIdAndPath.", null, null);
                    idffolder = null;
                    return idffolder;
                }
                idffolder = (IDfFolder)idfsession.newObject(folderObjectType);
            } else {
                idffolder = (IDfFolder)idfsession.newObject("dm_folder");
            }
            idffolder.setObjectName(as[i]);
            idffolder.link(s3);
            idffolder.save();
        }
        return idffolder;
    }

    private void LinkObject(IDfSysObject idfsysobject, String s, boolean flag) throws Exception {
         if (flag) {
            int i = idfsysobject.getFolderIdCount();
            for (int j = i - 1; j >= 0; j--) {
                idfsysobject.unlink(idfsysobject.getFolderId(j).toString());
            }
         }
         idfsysobject.link(s);
         idfsysobject.save();
    }
    
    private ITransformRequest createRenditionTransformRequestNew(IDfSession idfsession, IDfSysObject objectToTransform, IMediaProfile imediaprofile, 
    		String targetFormat, IProfileParameter aiprofileparameter[], IDfId idfid, boolean flag) throws Exception {
	    ITransformRequest itransformrequest = (ITransformRequest)idfsession.newObject("dm_transform_request");
	    itransformrequest.setSourceObjectId(objectToTransform.getObjectId().toString());
	    itransformrequest.setMediaProfileId(imediaprofile.getObjectId().toString());
	    itransformrequest.setSourceFormat(objectToTransform.getContentType());
	    itransformrequest.setTargetFormat(targetFormat);
		if(idfid != null)
	    	itransformrequest.setRelatedObjectId(idfid.toString());   
		else
		   itransformrequest.setRelatedObjectId(null);  
	    itransformrequest.setLocale(Locale.getDefault());
	    itransformrequest.setParameters(aiprofileparameter);
	    itransformrequest.setMediaProfileName(imediaprofile.getObjectName());
	    itransformrequest.setMediaProfileLabel(imediaprofile.getProfileLabel());
	    itransformrequest.setNotifyResult(imediaprofile.getNotifyResult());
	    itransformrequest.setDefaultProxy(flag);
	    itransformrequest.setPriority(1);
	    itransformrequest.setSourcePage(0);       
	    itransformrequest.setTargetPage(0);
	    itransformrequest.save();
	    return itransformrequest;      	    
    }
    public static class Result{
    	public String getErrorMessage() {
    		return errorMessage;
    	}
    	public void setErrorMessage(String errorMessage) {
    		this.errorMessage = errorMessage;
    	}
    	public Boolean getSuccess() {
    		return new Boolean(success);
    	}
    	public void setSuccess(boolean success) {
    		this.success = success;
    	}
    	public String getNewObjectId() {
    		return newObjectId;
    	}
    	public void setNewObjectId(String newObjectId) {
    		this.newObjectId = newObjectId;
    	}
    	private String errorMessage = "";
    	private boolean success = true;
    	private String newObjectId = ""; 
    }
}

