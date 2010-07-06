// validation

function validateFormOnSubmit(updateprofile) {
	var reason = "";

  	reason += validatePassword(updateprofile.password);
  	reason += validateEmail(updateprofile.email);
      
	if (reason != "") {
	    alert("Some fields need correction:\n" + reason);
	    return false;
	}

	return true;
}
function validateEmpty(fld) {
    var error = "";
 
    if (fld.value.length == 0) {
        fld.style.background = 'Yellow'; 
        error = "The required field has not been filled in.\n"
    } else {
        fld.style.background = 'White';
    }
    return error;  
}
function validateUsername(fld) {
    var error = "";
    var illegalChars = /\W/; // allow letters, numbers, and underscores
 
    if (fld.value == "") {
        fld.style.background = 'Yellow'; 
        error = "You didn't enter a username.\n";
    } else if ((fld.value.length < 1) || (fld.value.length > 15)) {
        fld.style.background = 'Yellow'; 
        error = "The username is the wrong length.\n";
    } else if (illegalChars.test(fld.value)) {
        fld.style.background = 'Yellow'; 
        error = "The username contains illegal characters.\n";
    } else {
        fld.style.background = 'White';
    }
    return error;
}
function validatePassword(fld) {
    var error = "";
    var illegalChars = /[\W_]/; // allow only letters and numbers 
 
    if (fld.value == "") {
        fld.style.background = 'Yellow';
        error = "You didn't enter a password.\n";
    } else if ((fld.value.length < 4) || (fld.value.length > 15)) {
        error = "The password is the wrong length. \n";
        fld.style.background = 'Yellow';
    } else if (illegalChars.test(fld.value)) {
        error = "The password contains illegal characters.\n";
        fld.style.background = 'Yellow';
    } else if (!((fld.value.search(/(a-z)+/)) && (fld.value.search(/(0-9)+/)))) {
        error = "The password must contain at least one numeral.\n";
        fld.style.background = 'Yellow';
    } else {
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
        fld.style.background = 'Yellow';
        error = "You didn't enter an email address.\n";
    } else if (!emailFilter.test(tfld)) {              //test email for illegal characters
        fld.style.background = 'Yellow';
        error = "Please enter a valid email address.\n";
    } else if (fld.value.match(illegalChars)) {
        fld.style.background = 'Yellow';
        error = "The email address contains illegal characters.\n";
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

