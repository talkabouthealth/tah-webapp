	$(document).ready(function() {
		var share = "${params.share}";
		if (share === "twitter") {
			showPopup("#shareTwitterDialog", 350);
		}
		else if (share === "facebook") {
			showPopup("#shareFBDialog", 450);
		}
		$("#followConvoBtn").click(function() {
			//replace More button with loading image
			var moreBtn = this;
			$("#ajaxLoading").appendTo(moreBtn.parentNode).show();
			moreBtn.style.display='none';
		
			$.post("@{Conversations.follow()}", 
				{ convoId: currentConvoId},
				function(nextAction) {
	  				if (nextAction === "follow") {
	  					$("#followConvoBtn").removeClass().addClass("followConvoBtn");
	  					$("#ajaxLoading").hide();
	  					$("#followConvoBtn").show();
	  				}
	  				else {
	  					$("#followConvoBtn").removeClass().addClass("followingConvoBtn");
	  					$("#ajaxLoading").hide();
	  					$("#followConvoBtn").show();
	  				}
				}
			);
		});
		$(".restartConvoLink").click(function() {
			openChat(tid);
			$.post("@{Conversations.restart}", {convoId: currentConvoId});
		});
		$(".flagConvoLink").click(function() {
			flagType = "convo";
			flagId = currentConvoId;
			showPopup("#flagPopup", 200);
		});
		//----- Answers ------
		//Vote Up/Vote Down handler
		$(".votebtnup, .votebtndown").live("click", function() {
			var value = $(this).hasClass("votebtnup") ? true : false;
			var answerId = $(this).attr("rel");

			$.post("@{Conversations.vote}", 
	   				{ answerId : answerId, up : value },
	   				function(data) {
		   				if (data !== "Error") {
	   		   				if (value) {
	   			   				$("#vote"+answerId+" .votebtnup").addClass("selected");
	   			   				$("#vote"+answerId+" .votebtndown").removeClass("selected");
	   		   				}
	   		   				else {
	   		   					$("#vote"+answerId+" .votebtnup").removeClass("selected");
	   			   				$("#vote"+answerId+" .votebtndown").addClass("selected");
	   		   				}
	   		   				$("#comment"+answerId+" .votestxt3").html(data);
		   				}
	   				}
				);

			return false;
		});
		$(".flagAnswerLink").live("click", function() {
			flagType = "answer";
			flagId = $(this).attr("rel");
			showPopup("#flagPopup", 200);
		});
		//Delete topic from topics (tags)
		$(".deleteTopicLink").live("click", function() {
			$(this).prev().remove();
			var topicId = $(this).attr("rel");
			$(this).remove();

			$.post("@{Conversations.updateField()}", 
					{ convoId : currentConvoId, name: 'topic', value: topicId, todo: 'remove'},
					function() {
						$("#topicsList").html($("#topicsEditList").html());
					}
				);

			return false;
		});
		//Delete disease from other diseases
		$(".deleteDiseaseLink").live("click", function() {
			$(this).prev().remove();
			var disease = $(this).attr("rel");
			$(this).remove();
			$.post("@{Conversations.updateField()}", 
					{ convoId : currentConvoId, name: 'disease', value: disease, todo: 'remove'},
					function() {
						$("#diseasesList").html($("#diseasesEditList").html());
					}
				);
			return false;
		});
		//Delete conversation from Related/Follow-Up conversations
		$(".deleteConvoLink").live("click", function() {
			var deleteConvoId = $(this).attr("rel");
			$(this).parent().remove();
			var name = "";
			if ($(this).hasClass("relatedConvos")) {
				name = "relatedConvos";
			} else {
	   			name = "followupConvos";
			}
			$.post("@{Conversations.updateField()}", 
				{ convoId : currentConvoId, name: name, value: deleteConvoId, todo: 'remove'},
				function(html) {
					if (name === "relatedConvos") {
						$("#relatedConvosList").html($("#relatedConvosEditList").html());
	   	   			} else {
	   	   				$("#followupConvosList").html($("#followupConvosEditList").html());
	   	   			}
				}
			);
			return false;
		});
		$("#mergeConvosBtn").click(function() {
			if (selectedConvoURL) {
				var confirmMerge = confirm("Are you sure you want to merge the following question: "+selectedConvoURL);
	   	   		if (confirmMerge) {
	   	   			showStatus("Merging...");
	   	   	  		$.post("@{Conversations.mergeConvos()}", 
	    				{ convoId : currentConvoId, convoToMergeURL : selectedConvoURL},
	    				function(html) {
	    					window.location.reload();
	    				}
	    			);
	   	   		}
			} else {
	   			alert("Please select correct conversation");
			}
			return false;
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
		makeDiseaseAutocomplete("#diseaseInput");
	});
	/** Called after inline editing of title/details/summary/etc. */

	function saveData(dataType, newValue) {
		if (dataType === 'titleEdit') {
			$.post("@{Conversations.updateField()}", 
				{ convoId : currentConvoId, name: 'title', value: newValue},
				function() {}
			);
		}
		else if (dataType === 'detailsEdit') {
			$.post("@{Conversations.updateField()}", 
				{ convoId : currentConvoId, name: 'details', value: newValue},
				function() {}
			);
		}
		else if (dataType === 'summaryEdit') {
			$.post("@{Conversations.updateField()}", 
				{ convoId : currentConvoId, name: 'summary', value: newValue},
				function() {}
			);
		}
		else if (dataType.indexOf('answerEdit') == 0) {
			var answerId = dataType.substring(10);
			
	   		$.post("@{Conversations.updateAnswer()}", 
				{ answerId: answerId, todo: 'update', newText: newValue},
				function(html) {}
			);
		}
	}
	/**
	* Called after editing lists - topics, related/follow-up conversations
	*/
	function updateData(dataType, newValue) {
		if (dataType === 'relatedConvosEdit') {
			$.post("@{Conversations.updateField()}", 
				{ convoId : currentConvoId, name: 'relatedConvos', value: newValue, todo: 'add'},
				function(html) {
					$(html).appendTo($("#relatedConvosEditList"));
					$("#relatedConvosList").html($("#relatedConvosEditList").html());
				}
			);
		} else if (dataType === 'followupConvosEdit') {
			$.post("@{Conversations.updateField()}", 
				{ convoId : currentConvoId, name: 'followupConvos', value: newValue, todo: 'add'},
				function(html) {
					$(html).appendTo($("#followupConvosEditList"));

					$("#followupConvosList").html($("#followupConvosEditList").html());
				}
			);
		} else if (dataType === 'topicsEdit') {
			//possible comma-separated topics
			$.post("@{Conversations.updateField()}", 
				{ convoId : currentConvoId, name: 'topic', value: newValue, todo: 'add'},
				function(html) {
					$(html).appendTo($("#topicsEditList"));

					$("#topicsList").html($("#topicsEditList").html());
				}
			);
		} else if (dataType === 'diseaseEdit') {
			//possible comma-separated diseases
			$.post("@{Conversations.updateField()}", 
				{ convoId : currentConvoId, name: 'disease', value: newValue, todo: 'add'},
				function(html) {
					$(html).appendTo($("#diseasesEditList"));

					$("#diseasesList").html($("#diseasesEditList").html());
				}
			);
		}
	}