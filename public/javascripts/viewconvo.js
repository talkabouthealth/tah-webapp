$(document).ready(function() {
	if (share === "twitter") {
		showPopup("#shareTwitterDialog", 350);
	} else if (share === "facebook") {
		showPopup("#shareFBDialog", 450);
	}
});
function addAnswer(){
	showPopup("#addAnswer", 350);
}