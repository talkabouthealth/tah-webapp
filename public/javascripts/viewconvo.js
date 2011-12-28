function startWindow(){
	if (share === "twitter") {
		showPopup("#shareTwitterDialog", 350);
	} else if (share === "facebook") {
		showPopup("#shareFBDialog", 450);
	}
	$(".flagAnswerLink").live("click", function() {
		flagType = "answer";
		flagId = $(this).attr("rel");
		showPopup("#flagPopup", 200);
	});
	$("#postFollowupLink").click(function() {
		return showStartConvoDialog("question");
	});
		$('.inline-edit').inlineEdit(
  		{ hover: '', saveFunction: saveData, updateFunction: updateData}
  	);
	makeTopicsAutocomplete("#topicInput");
   	makeAutocomplete("#relatedConvosInput", "convoedit");
   	makeAutocomplete("#followupConvosInput", "convoedit");
   	makeAutocomplete("#convoToMerge", "convoedit");
   	makeDiseaseAutocomplete("#diseaseInput",currentConvoId);
});
function addAnswer(){
	showPopup("#addAnswer", 350);
}
undeleteAnswer = function(answerId){
	var confirmDel = confirm("Are you sure want to undelete this answer");
	if (confirmDel) { undeleteAnswerMain(answerId); }
	return false;
}
function deleteAnswer(answerId) {
	var confirmDel = confirm("Are you sure want to delete this answer/reply?");
  	if (confirmDel) {
  		$("#comment"+answerId).remove();
  		deleteAnswerMain(answerId);
  	}
  	return false;
}
function markNotHelpful(answerId) {
  		var confirmDel = confirm("Are you sure you want to mark this answer as 'Not Helpful?'");
  		if (confirmDel) {
  			markNotHelpfulMain(answerId);
  		}
  		return false;
}
function deleteConvo() {
  		var confirmDel = confirm("Are you sure want to delete this conversation?");
  		if (confirmDel) {
  			deleteConvoMain();
  		}
  		return false;
}
function deleteChatMessage(index) {
	var confirmDel = confirm("Are you sure want to delete this message?");
	if (confirmDel) {
   		$("#chatMessage"+index).remove();
   		deleteChatMessageMain(index);
	}
	return false;
}