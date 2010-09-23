/*
 * Inline Text Editing 1.3
 * April 26, 2010
 * Corey Hart @ http://www.codenothing.com
 * 
 * Updated on Sep 16, 2010 by Kangaroo
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
	
			// Display Actions
			$editLink.bind( 'click.inline-edit', function(){
				$display.hide();
				$form.show();
				
				$text.val($view.html()).focus();

				/*
				if ( settings.html ) {
					if ( original === undefined ) {
						original = $view.html();
					}
					alert(original);
					$text.val( original ).focus();
				}
				else if ( original === undefined ) {
					original = $text.val();
				}
				*/

				return false;
			})
			.bind( 'mouseenter.inline-edit', function(){
				$display.addClass( settings.hover );
			})
			.bind( 'mouseleave.inline-edit', function(){
				$display.removeClass( settings.hover );
			});
			
			$addLink.bind( 'click.inline-edit', function(){
				$text.val("").focus();
				
				$display.hide();
				$form.show();
			});
			
			$addBtn.bind( 'click.inline-edit', function() {
				var newValue = $.trim($text.val());
				
				settings.updateFunction.apply( window, [ $dataType, newValue] );
			});
			
			// Add revert handler
			$revert.bind( 'click.inline-edit', function(){
				$text.val( original || '' ).focus();
				return false;
			});
			
			

			// Cancel Actions
			$cancel.bind( 'click.inline-edit', function(){
				$form.hide();
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
					alert('Incorrect value.');
					$text.focus();
					return false;
				}
				
				$form.hide();
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
				
				settings.saveFunction.apply( window, [ $dataType, newValue ] );

				
				/*
				$display.html( settings.loadtxt ).show();

				if ( $display.hasClass( settings.hover ) ) {
					$display.removeClass( settings.hover );
				}

				$.ajax({
					url: settings.href,
					type: settings.requestType,
					data: settings.postFormat ? 
						settings.postFormat.call( $main, event, { settings: settings, postData: settings.postData } ) :
						settings.postData,
					success: function( response ){
						original = undefined;

						if ( settings.load ) {
							settings.load.call( $display, event, { response: response, settings: settings } );
							return;
						}

						$display.html( response );
					}
				});
				*/

				return false;
			});
		});
	};

})( jQuery );
