package com.toastedsiopao.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "site_settings")
@Data
@NoArgsConstructor
public class SiteSettings {

	@Id
	private Long id = 1L;

	@Column(length = 50)
	private String websiteName = "MK Toasted Siopao";

	@Column(length = 255)
	private String carouselImage1 = "/img/carousel1.jpg";
	@Column(length = 255)
	private String carouselImage2 = "/img/carousel2.jpg";
	@Column(length = 255)
	private String carouselImage3 = "/img/carousel3.jpg";

	@Column(length = 100)
	private String featuredProductsTitle = "Our Featured Products";

	@Column(length = 255)
	private String featureCard1Image = "/img/favorites.jpg";
	@Column(length = 50)
	private String featureCard1Title = "Favorite";
	@Column(length = 200)
	private String featureCard1Text = "Our best-selling toasted siopao flavors, loved by everyone.";

	@Column(length = 255)
	private String featureCard2Image = "/img/combo.jpg";
	@Column(length = 50)
	private String featureCard2Title = "Combo";
	@Column(length = 200)
	private String featureCard2Text = "Perfectly paired combos with siopao, drinks, and more.";

	@Column(length = 255)
	private String featureCard3Image = "/img/siopao.jpg";
	@Column(length = 50)
	private String featureCard3Title = "Siopao";
	@Column(length = 200)
	private String featureCard3Text = "Freshly made toasted siopao, crispy outside, soft inside.";

	@Column(length = 255)
	private String featureCard4Image = "/img/drinks.jpeg";
	@Column(length = 50)
	private String featureCard4Title = "Drinks";
	@Column(length = 200)
	private String featureCard4Text = "Refreshing drinks to pair with your favorite siopao.";

	@Column(length = 100)
	private String promoTitle = "Special Promo!";
	@Column(length = 200)
	private String promoText = "Get 2 Toasted Siopao + 1 Drink for only ₱99";

	@Column(length = 100)
	private String whyUsTitle = "Why Toasted Siopao?";
	@Column(length = 255)
	private String whyUsText = "Crispy outside, savory inside! Perfect for your cravings — delicious, filling, and budget-friendly.";
	@Column(length = 255)
	private String whyUsImage = "/img/siopao.jpg";

	@Column(length = 100)
	private String aboutTitle = "About MK Toasted Siopao";
	@Column(length = 1000)
	private String aboutDescription1 = "MK Toasted Siopao started as a small family business with a passion for bringing warm, flavorful, and freshly toasted siopao to our local community. Our mission is simple, deliver high-quality siopao that everyone can enjoy, with love and care in every bite.";
	@Column(length = 1000)
	private String aboutDescription2 = "We take pride in our craftsmanship, customer service, and commitment to supporting local suppliers. At MK Toasted Siopao, every siopao is more than food, it’s a story, a tradition, and a taste you’ll remember.";
	@Column(length = 255)
	private String aboutImage = "/img/carousel1.jpg";

	@Column(length = 100)
	private String contactFacebookName = "MK Toasted Siopao";
	@Column(length = 255)
	private String contactFacebookUrl = "https://www.facebook.com/YourPage";
	@Column(length = 100)
	private String contactPhoneName = "+63 9XX XXX XXXX";
	@Column(length = 255)
	private String contactPhoneUrl = "tel:+639XXXXXXXXX";

	// --- MODIFIED: Simplified to a standard column to ensure ddl-auto works ---
	@Column(length = 255)
	private String footerText = "© 2025 MK Toasted Siopao | All Rights Reserved";
	// --- END MODIFIED ---
}