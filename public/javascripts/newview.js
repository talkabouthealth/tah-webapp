$(document).ready(function() {
	$('.inline-edit').inlineEdit( { hover: ''} );
});
function showSaveDialog(type) {
	if (type === 'startChat') {
		$("#newConvoTypeConvo").attr("checked", "checked");
	}
	else {
		$("#newConvoTypeQuestion").attr("checked", "checked");
	}
	
	$("#newConvoForm").show();
	$("#newQuestionConfirm").hide();
	$("#newTalkConfirm").hide();

	var convoTitle = $("#"+type+"Text").val();
	if (convoTitle !== "" 
			&& convoTitle !== "Post a question and we will notify the right members to answer."
			&& convoTitle !== "Start a live chat by sharing the topic you would like to discuss. Your peers will join within minutes." ) {
		$("#newConvoTitle").val($("#"+type+"Text").val());
	}
	showPopup("#startDialog", 350);
}
function joinNow(){
	showPopup("#profileJoinNow", 350);
}