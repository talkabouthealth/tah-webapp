/*
 * truncatable 1.2 - jQuery lightwieght text truncation plugin
 *
 * Copyright (c) 2009 Philip Beel (http://www.theodin.co.uk/)
 * Dual licensed under the MIT (http://www.opensource.org/licenses/mit-license.php) 
 * and GPL (http://www.opensource.org/licenses/gpl-license.php) licenses.
 *
 * Revision: $Id: jquery.truncatable.js 2009-08-20 $
 *
 * Upd: updated for handling also html text
 */

(function($){$.fn.truncatable=function(options){
		var defaults={limit:100,more:'...',less:false,hideText:'[read less]'};
		var options=$.extend(defaults,options);
		return this.each(function(num){
			//get size of only plain text, we don't need html tags
			if(($(this).html()).search(">... more")==-1){
				$( this ).linkify();
				var fullText = $(this).html();
				
				var plainText = $(this).text();
				//var plainText = $(this).text();
				if(plainText.indexOf("<a") <= defaults.limit && plainText.indexOf("</a") >= defaults.limit) {
					defaults.limit = plainText.indexOf("</a>");
				}
				var stringLength=plainText.length;
				if(stringLength>defaults.limit){
					var splitText=plainText.substr(defaults.limit);
					var splitPoint=splitText.substr(0,1);
					var whiteSpace=new RegExp(/^\s+$/);
					for(var newLimit=defaults.limit;newLimit<stringLength;newLimit++) {
						var newSplitText=$(this).html().substr(0,newLimit);
						var newHiddenText=$(this).html().substr(newLimit);
						var newSplitPoint=newSplitText.slice(-1);
						if(whiteSpace.test(newSplitPoint)){
							var hiddenText='<span class="hiddenText_'+num+'" style="display:none">'+fullText+'</span>';
							var setNewLimit=(newLimit-1);
							var trunkLink=$('<a>').attr('class','more_'+num+'');
							var smallText = '<span class="smallText_'+num+'" style="display:block;">'+plainText.substr(0,setNewLimit)+'</span>';
							$(this).html(smallText).append('<a class="more_'+num+'" href="#">'+defaults.more+'<a/> '+hiddenText);
							$('a.more_'+num).bind('click',function(){
								$('span.smallText_'+num).hide();
								$('span.hiddenText_'+num).show();
								$('a.more_'+num).hide();
								if(defaults.less==true) {
									$('span.hiddenText_'+num).append('<a class="hide_'+num+'" href="" title="'
											+ defaults.hideText+'">'+defaults.hideText+'</a>');
									$('a.hide_'+num).bind('click',function(){
										$('.hiddenText_'+num).hide();
										$('span.smallText_'+num).show();
										$('.more_'+num).show();
										$('.hide_'+num).empty();
										return false
									})
								}
							return false;
							});
							newLimit=stringLength
						}
					}
				}
			}
	    }
	)}
})(jQuery);
