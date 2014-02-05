function getUploadModes() {
	return [
		"My Computer|localFile",
		"My Files|myFiles"
	];
}

function getUploadModeString() {
	if(viewScope.uploadMode==null) {
		viewScope.uploadMode=getUploadModes()[1];
	}

	return viewScope.uploadMode;
}

function getUploadModeTitle() {
	var uploadMode=getUploadModeString();
	
	var mode= @Right(uploadMode, "|");
	var label=@Left(uploadMode, "|");
	
	if(mode=="localFile") {
		return "From <b>"+label+"</b>";
	} else if(mode=="myFiles") {
		return "From <b>"+label+"</b>";
	} else {
		return "From <b>"+label+"</b>";
	}
	
}

function getFileListMode() {
	var uploadMode=getUploadModeString();
	
	var mode= @Right(uploadMode, "|");
	
	if(mode=="localFile") {
		return "local";
	} else {
		return "connections";
	}
	
}

function getFileList() {
	var mode= @Right(getUploadModeString(), "|");
	
	if(mode=="myFiles") {
		return ics.getFiles();
	} else {
		return ics.getCommunityFiles(mode);
	}
	
}

function getSelectedProjectTitle() {
	if(viewScope.selectedProject==null) {
		viewScope.selectedProject=bcs.getProjectsForCombo().get(0);
	}
	
	return "...to: <b>"+@Left(viewScope.selectedProject, "|")+"</b>";
}