import streamlit as st
import math

# 1. The Title
st.title(" 🏒 Airow Sports Model")
st.write("Live from Samsung S25 Ultra")

# 2. Input Section
st.header("Team Strength Calculator")
home_rating = st.slider("Home Team Rating", 1, 100, 50)
away_rating = st.slider("Away Team Rating", 1, 100, 50)

# 3. The Math (Simple Logic for now)
# If Home is 60 and Away is 40, diff is 20.
diff = home_rating - away_rating
win_prob = 50 + (diff * 0.5)

# 4. The Output
st.metric(label="Home Win Probability", value=f"{win_prob}%")

if win_prob > 60:
    st.success("Bet on Home Team!")
    elif win_prob < 40:
        st.error("Bet on Away Team!")
        else:
            st.warning("Too close to call.")
            