/* Scripts for all pages */
var selectedTalkerId = '';

function openChat(convoId){
	window.open("/talk/"+convoId, "TalkerWindow", "width=730,height=565");
}

//createThankYou(String toTalker, String note)
function sendThankYou() {
	var noteText = $("#thankYouListNote").val();
	$.fancybox.close();
	$("#thankYouListNote").val("");

	$.post("/actions/createThankYou", 
		{ toTalker: selectedTalkerId, note: noteText}
	);
}

//saveProfileComment(String profileTalkerId, String parentId, String text)
function sendProfileComment() {
	var commentText = $("#commentText").val();
	$.fancybox.close();
	$("#commentText").val("");

	$.post("/actions/saveProfileComment", 
		{ profileTalkerId: selectedTalkerId, parentId: '', text: commentText}
	);
}

$(document).ready(function() {
	
	//links to show more content (e.g. full bio)
	$(".more11").live("click", 
		function() {
			$(this)
				.parent().hide()
				.next(".fulltext").show();
		});
	
	$(".followActionLink").live('click', 
		function() {
			var followingId = $(this).attr("rel");
			var followLink = $(this);
  			$.post("/actions/follow", 
 				{ followingId: followingId },
 				function(data) {
 					followLink.html(data);
 				}
 			);
		});
	
	//Dialog popus
	//Hack to use Fancybox with 'live' event - fancybox on mouseover
	$(".commentLink, .thankYouLink").live('mouseover', function(event) {
			selectedTalkerId = $(this).attr("rel2");
			$(this).fancybox({
				    'hideOnContentClick': false,
				    'showCloseButton': false
				});
		})
});



