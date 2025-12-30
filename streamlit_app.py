import streamlit as st

st.title("🏒 Airow Sports Model")

# --- INPUTS ---
st.header("Win Probability Calculator")
home_rating = st.slider("Home Team Rating", 1200, 1800, 1500)
away_rating = st.slider("Away Team Rating", 1200, 1800, 1500)

# --- MATH ---
diff = away_rating - home_rating
win_prob = 1 / (1 + 10 ** (diff / 400))

# --- OUTPUT ---
st.metric(label="Home Win Probability", value=f"{win_prob:.1%}")

if win_prob > 0.60:
    st.success("Recommendation: BET HOME")
elif win_prob < 0.40:
    st.error("Recommendation: BET AWAY")
else:
    st.warning("No clear edge")
