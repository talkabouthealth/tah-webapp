/*
 * Inline Text Editing 1.3
 * April 26, 2010
 * Corey Hart @ http://www.codenothing.com
 * 
 * Updated on Sep 16, 2010 by Kangaroo
 * Some elements were changed for TAH. 
 * Added support for editing lists (e.g. tags, related conversations).
 */ 
(function( $, undefined ){

	$.fn.inlineEdit = function( options ) {
		return this.each(function(){
			// Settings and local cache
			var self = this, $main = $( self ), original,
				settings = $.extend({
					href: 'ajax.php',
					requestType: 'POST',
					html: true,
					load: undefined,
					display: '.inline_display',
					editlink: '.inline_editlink',
					form: '.inline_form',
					addlink: '.inline_addlink',
					text: '.inline_text',
					save: '.inline_save',
					cancel: '.cancel',
					revert: '.revert',
					loadtxt: 'Loading...',
					hover: undefined,
					postVar: 'text',
					postData: {},
					saveFunction: undefined,
					updateFunction: undefined,
					postFormat: undefined
				}, options || {}, $.metadata ? $main.metadata() : {} ),

				// Cache All Selectors
				$display = $main.find( settings.display ),
				$form = $main.find( settings.form ),
				$text = $form.find( settings.text ),
				$save = $form.find( settings.save ),
				$revert = $form.find( settings.revert ),
				$cancel = $form.find( settings.cancel ),
				$addBtn = $form.find('.inline_add'),
				$doneBtn = $form.find('.inline_done'),
				$view = $main.find('.inline_view'),
				$editLink = $main.find('.inline_editlink'),
				$addLink = $main.find('.inline_addlink'),
				$displayFull = $main.find('.inline_full'),
				$displayEmpty = $main.find('.inline_empty'),
				$dataType = $main.attr('id');

			// Make sure the plugin only get initialized once
			if ( $.data( self, 'inline-edit' ) === true ) {
				return;
			}
			$.data( self, 'inline-edit', true );

			// Prevent sending form submission
			$form.bind( 'submit.inline-edit', function(){
				$save.trigger( 'click.inline-edit' );
				return false;
			});
	
			// Edit link
			$editLink.live( 'click.inline-edit', function(){
				$display.hide();
				$form.show();
				var editText = "";
				if ($view.size() != 0) {
					editText = $view.html();
				}
				if (editText) {
					editText = editText.replace(/<br>/g, '\n');
					editText = editText.replace(/<BR>/g, '\n');

					//'more' link adds html code, before editing we need to remove it
					//<a class="more_0">... more</a> <span class="hiddenText_1" style="display:none">...</span>
					editText = editText.replace(/<a class="more.*?<\/a>/g, '');
					editText = editText.replace(/<span[^>]*>/g, '');
					editText = editText.replace(/<\/span>/g, '');
					
					//remove links html code before editing
					editText = editText.replace(/<a[^>]*>/g, '');
					editText = editText.replace(/<\/a>/g, '');
				}
				$text.val(editText).focus();

				return false;
			})
			.bind( 'mouseenter.inline-edit', function(){
				$display.addClass( settings.hover );
			})
			.bind( 'mouseleave.inline-edit', function(){
				$display.removeClass( settings.hover );
			});

			// Add new value if current is empty
			$addLink.bind( 'click.inline-edit', function(){
				$text.val("").focus();
				
				$display.hide();
				$form.show();
				return false;
			});
			
			// Add new value to the edited list (e.g. a new tag to the list of tags)
			$addBtn.bind( 'click.inline-edit', function() {
				var newValue = $.trim($text.val());
				$text.val("");
				
				//callback for updating list on the page
				settings.updateFunction.apply( window, [ $dataType, newValue] );
			});
			
			// Add revert handler
			$revert.bind( 'click.inline-edit', function(){
				$text.val( original || '' ).focus();
				return false;
			});

			// Cancel or Done link
			$cancel.bind( 'click.inline-edit', function(){
				$form.hide();

				if ($cancel.html() == "Done") {
					//if form has only one link ('Done' link) - 
					//it means that list is empty, whe show empty text
					if ($form.find("a").size() === 1) {
						$displayFull.hide();
						$displayEmpty.show();
					}
					else {
						$displayEmpty.hide();
						$displayFull.show();
					}
				}
				$display.show();

				// Remove hover action if stalled
				if ( $display.hasClass( settings.hover ) ) {
					$display.removeClass( settings.hover );
				}
				
				return false;
			});

			// Save Actions
			$save.bind( 'click.inline-edit', function( event ) {
				var newValue = $.trim($text.val());
				if ($text.hasClass('notempty') && newValue === '') {
					//if value should be notempty - validate and show error message
					alert('Incorrect value.');
					$text.focus();
					return false;
				}
				
				$form.hide();
				
				if ($dataType.indexOf('commentEdit') == 0) {
					var commentId = $dataType.substring(11);
					
		   	   		$.post("/actions/updateComment", 
		  				{ commentId: commentId, newText: newValue},
		  				function(returnText) {		  					
							$view.html(returnText);
							
							$displayEmpty.hide();
							$displayFull.show();
							$display.show();
		  				}
		  			);		  			
				}
				else if ($dataType.indexOf('answerEdit') == 0) {
					var answerId = $dataType.substring(10);
					
		   	   		$.post("/conversations/updateAnswer", 
		  				{ answerId: answerId, todo: 'update', newText: newValue},
		  				function(returnText) {
		  					$view.html(returnText);
							
							$displayEmpty.hide();
							$displayFull.show();
							$display.show();
		  				}
		  			);
				}
				else {
					//prepare plain text for display
					newValue = linkify(newValue);
					newValue = newValue.replace(/\n/g, '<br/>');
					$view.html(newValue);
					
					if (newValue === '') {
						$displayFull.hide();
						$displayEmpty.show();
					}
					else {
						$displayEmpty.hide();
						$displayFull.show();
					}
					$display.show();
					
					if (settings.saveFunction) {
						//callback for saving new value on the server
						settings.saveFunction.apply( window, [ $dataType, newValue ] );
					}
				}								
				
				return false;
			});
		});
	};

})( jQuery );
