/**
 * jQuery JavaScript
 * Author: HCL(Chandan Sihna)
 * Date: 07/04/2009
**/
jQuery.jFastMenu = function(id){
	$(id + ' ul li').hover(function(){
		$(this).find('ul:first').animate({height:'show'}, 'fast');
	},
	function(){
		$(this).find('ul:first').animate({height:'hide', opacity:'hide'}, 'slow');
	});

}