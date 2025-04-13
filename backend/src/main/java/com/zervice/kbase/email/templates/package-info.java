package com.zervice.kbase.email.templates;

/**
 * All those files are borrowed from https://github.com/rocketbase-io/email-template-builder
 * (only uses email-template-builder) for building our email templates
 *
 * We are mixing the Pebble engine with above mentioned templates system to create beautiful emails.
 * The above email template system tries to "programatically" define the email content (with possible varialbes in it),
 * and Pebble would then evaluate the template to generate actual HTML email based on a pre-set template
 *    templates/email/layout.html for html email
 *    templates/email/layout.txt  for plain email
 */
