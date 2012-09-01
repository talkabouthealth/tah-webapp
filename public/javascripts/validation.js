/*
 	SignUp validation. Most methods aren't used - left for possible future use.
 */
function validateFormOnSubmit(signup) {
	var reason = "";
	_gaq.push(['_trackEvent', 'Signup_page', 'Signup', 'Standard']);
	  //reason += validateUsername(signup.username);
	  //reason += validatePassword(signup.password);
	  //reason += validateEmail(signup.email);
	  //reason += validateMonth(signup.month);  
	  //reason += validateDay(signup.day);  
	  //reason += validateYear(signup.year);  
	 // reason += validateGender(signup.gender);  
	//  reason += validateAge(signup.age);  
		
	  if (reason != "") {
	    alert("Some fields need correction:\n" + reason);
	    return false;
	  }
	
	  // alert("All fields are filled correctly");
	  document.getElementById("signup").submit();
}
function validateEmpty(fld) {
    var error = "";
 
    if (fld.value.length == 0) {
        fld.style.background = 'orange'; 
        error = "The required field has not been filled in.\n"
    } else {
        fld.style.background = 'White';
    }
    return error;  
}
function validateUsername(fld) {
    var error = "";

    var varUN = new RegExp("^[\\w-_\\.]{1,25}$");
    if (fld.value == "") {
        fld.style.background = 'orange'; 
        error = "You didn't enter a username.\n";
    } else if ((fld.value.length < 3) || (fld.value.length > 25)) {
        fld.style.background = 'orange'; 
        error = "Username must be between 3 and 25 Characters.\n";
    } else if (fld.value.search(varUN) == -1) {
        fld.style.background = 'orange'; 
        error = "The username contains illegal characters. Only letters, numbers, underscores, dashes, and periods are allowed.\n";
    } else {
        fld.style.background = 'White';
    }
    /*varUN = new RegExp("^[\\w-_\\.]{1,25}$");
    if(fld.value.search(varUN) == -1){
    	fld.style.background = 'orange'; 
        error = "The username contains illegal characters. Only letters, numbers, underscore, dash, and period are allowed.\n";
    } else if (illegalChars.test(fld.value)) {
        fld.style.background = 'orange'; 
        error = "The username contains illegal characters. Only letters, numbers, underscore, dash, and period are allowed.\n";
    } var illegalChars = /\W/; // allow letters, numbers, and underscores
    */
    return error;
}

function validatePassword(fld) {
    var error = "";
    var varPW = new RegExp("^[\\w-_\\.?!@#$%^&*()+=]{1,25}$");
    if (fld.value == "") {
        fld.style.background = 'orange';
        error = "You didn't enter a password.\n";
    } else if ((fld.value.length < 4) || (fld.value.length > 25)) {
        error = "Password must be between 3 and 25 Characters. \n";
        fld.style.background = 'orange';
    } else if (fld.value.search(varPW) == -1) {
        fld.style.background = 'orange'; 
        error = "The username contains illegal characters. Only letters, numbers, and the following characters are allowed: !@#$%^&*()_-+=?\.n";
    } /*else if (illegalChars.test(fld.value)) {
        error = "The password contains illegal characters. Only numbers, letters, underscore, dash, and period allowed.  \n";
        fld.style.background = 'orange';
    } /*else if (!((fld.value.search(/(a-z)+/)) && (fld.value.search(/(0-9)+/)))) {
        error = "The password must contain at least one numeral.\n";
        fld.style.background = 'orange';
    } var illegalChars = /[\W_]/; // allow only letters and numbers 
	  */else {
        fld.style.background = 'White';
    }
   return error;
}  
function trim(s)
{
  return s.replace(/^\s+|\s+$/, '');
}
function validateEmail(fld) {
    var error="";
    var tfld = trim(fld.value);                        // value of field with whitespace trimmed off
    var emailFilter = /^[^@]+@[^@.]+\.[^@]*\w\w$/ ;
    var illegalChars= /[\(\)\<\>\,\;\:\\\"\[\]]/ ;
   
    if (fld.value == "") {
        fld.style.background = 'orange';
        error = "You didn't enter an email address.\n";
    } else if (!emailFilter.test(tfld)) {              //test email for illegal characters
        fld.style.background = 'orange';
        error = "Please enter a valid email address.\n";
    } else if (fld.value.match(illegalChars)) {
        fld.style.background = 'orange';
        error = "The email address contains illegal characters.\n";
    } else {
        fld.style.background = 'White';
    }
    return error;
}

function validateMonth(fld) {
    var error = "";
 
    if (fld.value == 0) {
        fld.style.background = 'orange'; 
        error = "Please select your birth month.\n";
    } else {
        fld.style.background = 'White';
    }
    return error;
}
function validateDay(fld) {
    var error = "";
 
    if (fld.value == 0) {
        fld.style.background = 'orange'; 
        error = "Please select your birth date.\n";
    } else {
        fld.style.background = 'White';
    }
    return error;
}
function validateYear(fld) {
    var error = "";
 
    if (fld.value == 0) {
        fld.style.background = 'orange'; 
        error = "Please select your birth year.\n";
    } else {
        fld.style.background = 'White';
    }
    return error;
}		

function validateGender(btn) {
    var error = "";
    var cnt = -1;

    for (var i=btn.length-1; i > -1; i--){
     if (btn[i].checked) {cnt = i; i = -1;}
 	 } if (cnt > -1) {error = "";
 	 } else {error = "Please choose a your gender.\n";
 	 }
 	 return error;
}
               
function validateAge(fld)  {
	var error = "";
	
	if (!document.getElementById("age").checked){
        fld.style.background = 'orange'; 
        error = "You must agree to the Terms of Service and Age requirements";
	} else {
        fld.style.background = 'White';
	}
	return error;
}	


function validatePhone1(fld) {
    var error = "";
    var NaNphonefilter_phone1 = /^[2-9][0-9]{2}/
    
   if (fld.value == "") {
        error = "Please enter an area code.\n";
        fld.style.background = 'orange';
    } else if (!NaNphonefilter_phone1.test(fld.value)) {
	    error = "Please enter a valid area code.\n";
	    fld.style.background = 'Yellow';
	}
    return error;
}

function validatePhone2(fld) {
    var error = "";
    var NaNphonefilter_phone2 = /^[2-9][0-9]{2}/
    
   if (fld.value == "") {
        error = "Please enter a phone number.\n";
        fld.style.background = 'orange';
    } else if (!NaNphonefilter_phone2.test(fld.value)) {
	    error = "Please enter a valid phone number.\n";
	    fld.style.background = 'Yellow';
	}
    return error;
}    

function validatePhone3(fld) {
    var error = "";
    var NaNphonefilter_phone3 = /^[0-9]{4}/
    
   if (fld.value == "") {
        error = "Please enter a phone number.\n";
        fld.style.background = 'orange';
    } else if (!NaNphonefilter_phone3.test(fld.value)) {
	    error = "Please enter a valid phone number.\n";
	    fld.style.background = 'Yellow';
	}
    return error;
}    

